/** (C) Copyright 2013 Hal Hildebrand, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package com.hellblazer.autoconfigure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.nexus.GossipScope;
import com.hellblazer.slp.InvalidSyntaxException;
import com.hellblazer.slp.ServiceEvent;
import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;
import com.hellblazer.utils.LabeledThreadFactory;
import com.hellblazer.utils.Rendezvous;
import com.hellblazer.utils.Utils;

/**
 * A simple way to hack around the configuration shit storm of hell that is the
 * current state of distributed systems.
 * 
 * @author hhildebrand
 * 
 */
public class ConfigureMe {
	private static final Logger logger = Logger.getLogger(ConfigureMe.class
			.getCanonicalName());

	private final int addressIndex;
	private final AtomicReference<InetSocketAddress> bound = new AtomicReference<>();
	private final List<File> configurations;
	private final ServiceScope discovery;
	private final String hostVariable;
	private final String networkInterface;
	private final String portVariable;
	private final AtomicReference<Rendezvous> rendezvous = new AtomicReference<>();
	private final Map<ServiceListener, ServiceCollection> serviceCollections = new HashMap<>();
	private final String serviceFormat;
	private final Map<String, String> serviceProperties;
	private final Map<ServiceListener, Service> services = new HashMap<>();
	private final Map<String, String> substitutions;

	/**
	 * Construct an instance from the configuration POJO
	 * 
	 * @param config
	 *            - the configuration to use
	 * @throws SocketException
	 *             - if the discovery service cannot be constructed
	 */
	public ConfigureMe(AutoConfiguration config) throws SocketException {
		this(config.serviceUrl, config.hostVariable, config.portVariable,
				config.networkInterface, config.addressIndex,
				config.serviceProperties, new GossipScope(
						config.gossip.construct()), config.services,
				config.serviceCollections, config.configurations,
				config.substitutions);
	}

	/**
	 * Construct an instance
	 * 
	 * @param serviceFormat
	 *            - the format string which results in the service registration
	 *            URL
	 * @param hostVariable
	 *            - the name of the variable to substitute the host of this
	 *            service in the configuration files
	 * @param portVariable
	 *            - the name of the variable to substitute the allocated port of
	 *            this service in the configuation files
	 * @param networkInterface
	 *            - the network interface to use to bind this service
	 * @param addressIndex
	 *            - the index of the address to use that are bound to the
	 *            network interface
	 * @param serviceProperties
	 *            - the properties used to register this service in the
	 *            discovery scope
	 * @param discovery
	 *            - the service scope used for the auto configuration process
	 * @param serviceDefinitions
	 *            - the list of singular services that need to be discovered
	 * @param serviceCollectionDefinitions
	 *            - the list of service collections that need to be discovered
	 * @param configurations
	 *            - the list of files to process when all the required services
	 *            have been discovered
	 * @param substitutions
	 *            - an additional list of properties that will be substituted in
	 *            the configuration files
	 */
	public ConfigureMe(String serviceFormat, String hostVariable,
			String portVariable, String networkInterface, int addressIndex,
			Map<String, String> serviceProperties, ServiceScope discovery,
			List<Service> serviceDefinitions,
			List<ServiceCollection> serviceCollectionDefinitions,
			List<File> configurations, Map<String, String> substitutions) {
		this.serviceFormat = serviceFormat;
		this.hostVariable = hostVariable;
		this.portVariable = portVariable;
		this.networkInterface = networkInterface;
		this.addressIndex = addressIndex;
		this.serviceProperties = serviceProperties;
		this.discovery = discovery;
		this.configurations = configurations;
		this.substitutions = substitutions;
		for (Service service : serviceDefinitions) {
			services.put(serviceListener(), service);
		}
		for (ServiceCollection collection : serviceCollectionDefinitions) {
			serviceCollections.put(serviceCollectionListener(), collection);
		}
	}

	/**
	 * Run the auto configuration process.
	 * 
	 * @param success
	 *            - the closure to evaluate upon successful auto configuration
	 * @param failure
	 *            - the closure to evaluate upon failure to auto configure
	 * @param timeout
	 *            - the length of time to wait for auto configuration to
	 *            complete
	 * @param unit
	 *            - the unit of the wait time
	 */
	public void configure(Runnable success, Runnable failure, long timeout,
			TimeUnit unit) {
		logger.info("Beginning auto configuration process");
		Runnable successAction = successAction(success, failure);
		int cardinality = getCardinality();
		if (!rendezvous.compareAndSet(null, new Rendezvous(cardinality,
				successAction, failureAction(failure)))) {
			throw new IllegalStateException("System is already configuring!");
		}
		try {
			registerService();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Unable to register this service!", e);
			failure.run();
			return;
		}

		if (cardinality == 0) { 
			// no services required
			successAction.run();
			return;
		}

		rendezvous
				.get()
				.scheduleCancellation(
						timeout,
						unit,
						Executors
								.newSingleThreadScheduledExecutor(new LabeledThreadFactory(
										"Auto Configuration Scheduling Thread")));
		try {
			registerListeners();
			registerService();
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error registering service listeners", e);
			failure.run();
		}
	}

	protected void allocatePort() {
		NetworkInterface iface;
		try {
			iface = NetworkInterface.getByName(networkInterface);
		} catch (SocketException e) {
			String msg = String.format(
					"Unable to obtain network interface[%s]", networkInterface);
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalStateException(msg, e);
		}
		if (iface == null) {
			String msg = String.format("Unable to find network interface [%s]",
					networkInterface);
			logger.severe(msg);
			throw new IllegalStateException(msg);
		}
		try {
			if (!iface.isUp()) {
				String msg = String.format("Network interface [%s] is not up!",
						networkInterface);
				logger.severe(msg);
				throw new IllegalStateException(msg);
			}
		} catch (SocketException e) {
			String msg = String.format(
					"Unable to determin if network interface [%s] is up",
					networkInterface);
			logger.severe(msg);
			throw new IllegalStateException(msg);
		}
		logger.info(String.format("Network interface [%s] is up",
				iface.getDisplayName()));
		Enumeration<InetAddress> interfaceAddresses = iface.getInetAddresses();
		InetAddress raw = null;
		for (int i = 0; i <= addressIndex; i++) {
			if (!interfaceAddresses.hasMoreElements()) {
				String msg = String
						.format("Unable to find any network address for interface[%s] {%s}",
								networkInterface, iface.getDisplayName());
				logger.severe(msg);
				throw new IllegalStateException(msg);
			}
			raw = interfaceAddresses.nextElement();
		}
		if (raw == null) {
			String msg = String
					.format("Unable to find any network address for interface[%s] {%s}",
							networkInterface, iface.getDisplayName());
			logger.severe(msg);
			throw new IllegalStateException(msg);
		}
		InetAddress address;
		try {
			address = InetAddress.getByName(raw.getCanonicalHostName());
		} catch (UnknownHostException e) {
			String msg = String
					.format("Unable to resolve network address [%s] for interface[%s] {%s}",
							raw, networkInterface, iface.getDisplayName());
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalStateException(msg, e);
		}
		int port = Utils.allocatePort(address);
		if (port <= 0) {
			String msg = String.format(
					"Unable to allocate port on address [%s]", address);
			logger.severe(msg);
			throw new IllegalStateException(msg);
		}
		InetSocketAddress boundAddress = new InetSocketAddress(address, port);
		logger.info(String.format("Binding this service to [%s]", boundAddress));
		bound.set(boundAddress);
	}

	protected void discover(ServiceReference reference, Service service) {
		logger.info(String.format("discovered [%s] for service [%s]",
				reference.getUrl(), service));
		service.discover(reference);
	}

	protected void discover(ServiceReference reference,
			ServiceCollection serviceCollection) {
		logger.info(String.format(
				"discovered [%s] for service collection [%s]",
				reference.getUrl(), serviceCollection));
		serviceCollection.discover(reference);
	}

	protected Runnable failureAction(final Runnable failure) {
		return new Runnable() {
			@Override
			public void run() {
				logger.severe("Auto configuration failed due to not all services being discovered");
				for (Service service : services.values()) {
					if (!service.isDiscovered()) {
						logger.severe(String
								.format("Service [%s] has not been discovered",
										service));
					}
				}
				for (ServiceCollection serviceCollection : serviceCollections
						.values()) {
					if (!serviceCollection.isSatisfied()) {
						int cardinality = serviceCollection.cardinality;
						int discoveredCardinality = serviceCollection
								.getDiscoveredCardinality();
						logger.severe(String
								.format("Service collection [%s] has not been satisfied, missing %s services",
										serviceCollection, cardinality
												- discoveredCardinality,
										cardinality));
					}
				}
				failure.run();
			}
		};
	}

	protected int getCardinality() {
		int cardinality = 0;
		cardinality += services.size();
		for (ServiceCollection collection : serviceCollections.values()) {
			cardinality += collection.cardinality;
		}
		logger.info(String.format("Expecting %s service registrations",
				cardinality));
		return cardinality;
	}

	protected Map<String, String> getPropertySubstitutions() {
		Map<String, String> properties = new HashMap<>();
		// First the system properties
		for (Map.Entry<Object, Object> entry : System.getProperties()
				.entrySet()) {
			properties.put(String.valueOf(entry.getKey()),
					String.valueOf(entry.getValue()));
		}

		// Add any substitutions supplied
		properties.putAll(substitutions);

		// Add the bound host:port of this service
		properties.put(hostVariable, bound.get().getAddress().toString());
		properties.put(portVariable, Integer.toString(bound.get().getPort()));

		// Add all the substitutions for the service collections
		for (ServiceCollection serviceCollection : serviceCollections.values()) {
			properties.put(serviceCollection.variable,
					serviceCollection.resolve());
		}

		// Add all the substitutions for the services
		for (Service service : services.values()) {
			properties.put(service.variable, service.resolve());
		}
		logger.info(String
				.format("Using property substitions [%s]", properties));
		return properties;
	}

	/**
	 * Process the configuration file's content by creating a new version of it,
	 * substituting the variables necessary.
	 * 
	 * @param tempDir
	 *            - the directory to receive
	 * @param configFile
	 * @param propertySubstitutions
	 *            - the map of variables to replace in the configuration file
	 * @return the File containing the processed content
	 */
	protected File process(File tempDir, File configFile,
			Map<String, String> propertySubstitutions) {
		File destination;
		try {
			destination = File
					.createTempFile(configFile.getName(), "processed");
		} catch (IOException e) {
			String msg = String
					.format("Cannot create temporary file for processing the configuration file [%s]",
							configFile.getAbsolutePath());
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalStateException(msg, e);
		}
		InputStream is = null;
		OutputStream os = null;
		try {
			try {
				is = new FileInputStream(configFile);
			} catch (FileNotFoundException e) {
				String msg = String.format(
						"Cannot open configuration file [%s] for processing",
						configFile.getAbsolutePath());
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalStateException(msg, e);
			}
			try {
				os = new FileOutputStream(destination);
			} catch (FileNotFoundException e) {
				String msg = String
						.format("Cannot open temporary file [%s] for processing the configuration file [%s] for processing",
								destination.getAbsolutePath(),
								configFile.getAbsolutePath());
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalStateException(msg, e);
			}
			try {
				Utils.replaceProperties(is, os, serviceProperties);
			} catch (IOException e) {
				String msg = String.format(
						"Error processing the configuration file [%s] > [%s]",
						configFile.getAbsolutePath(),
						destination.getAbsolutePath());
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalStateException(msg, e);
			}
			logger.info(String.format("processed configuration file []",
					configFile.getAbsolutePath()));
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					logger.log(Level.FINEST, String.format(
							"cannot close processed stream [%s]",
							destination.getAbsolutePath()));
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.log(Level.FINEST, String.format(
							"cannot close configuration file stream [%s]",
							configFile.getAbsolutePath()));
				}
			}
		}

		return destination;
	}

	protected void processConfigurations(Map<String, String> propertySubstitions) {
		Map<File, File> processedConfigurations = new HashMap<>();
		File tempDir;
		try {
			tempDir = File.createTempFile("autoconfigure", "dir");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to create a temporary directory",
					e);
			throw new IllegalStateException(
					"Unable to create a temporary directory", e);
		}

		Utils.initializeDirectory(tempDir);

		try {
			for (File configFile : configurations) {
				if (!configFile.exists()) {
					logger.info(String.format(
							"missing configuration file [%s]",
							configFile.getAbsolutePath()));
					break;
				}
				processedConfigurations.put(configFile,
						process(tempDir, configFile, propertySubstitions));
			}
		} finally {
			Utils.remove(tempDir);
		}
		for (Map.Entry<File, File> entry : processedConfigurations.entrySet()) {
			try {
				Utils.copy(entry.getKey(), entry.getValue());
				logger.info(String.format(
						"copied processed configuration file []", entry
								.getValue().getAbsolutePath()));
			} catch (IOException e) {
				String msg = String
						.format("Cannot copy processed configuration [%s] to original location [%s]",
								entry.getValue().getAbsolutePath(), entry
										.getKey().getAbsolutePath());
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
	}

	protected void registerListeners() {
		registerServiceCollectionListeners();
		registerServiceListeners();
	}

	protected void registerService() {
		allocatePort();
		String service = String.format(serviceFormat,
				bound.get().getHostName(), bound.get().getPort());
		try {
			ServiceURL url = new ServiceURL(service);
			logger.info(String.format(
					"Registering this service as [%s] with properties [%s]",
					url, serviceProperties));
			discovery.register(url, serviceProperties);
		} catch (MalformedURLException e) {
			String msg = String.format("Invalid syntax for service URL [%s]",
					service);
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalArgumentException(msg, e);
		}
	}

	protected void registerServiceCollectionListeners() {
		for (Map.Entry<ServiceListener, ServiceCollection> entry : serviceCollections
				.entrySet()) {
			ServiceCollection service = entry.getValue();
			try {
				logger.info(String.format(
						"Registering listener for service collection [%s]",
						service));
				discovery.addServiceListener(entry.getKey(),
						service.constructFilter());
			} catch (InvalidSyntaxException e) {
				String msg = String
						.format("Invalid syntax for discovered service collection [%s]",
								service);
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalArgumentException(msg, e);
			}
		}
	}

	protected void registerServiceListeners() {
		for (Map.Entry<ServiceListener, Service> entry : services.entrySet()) {
			Service service = entry.getValue();
			try {
				logger.info(String.format(
						"Registering listener for service [%s]", service));
				discovery.addServiceListener(entry.getKey(),
						service.constructFilter());
			} catch (InvalidSyntaxException e) {
				String msg = String.format(
						"Invalid syntax for discovered service [%s]", service);
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalArgumentException(msg, e);
			}
		}
	}

	protected ServiceListener serviceCollectionListener() {
		return new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference reference = event.getReference();
				switch (event.getType()) {
				case REGISTERED:
					ServiceCollection serviceCollection = serviceCollections
							.get(this);
					if (serviceCollection == null) {
						String msg = String.format(
								"No existing listener matching [%s]",
								reference.getUrl());
						logger.severe(msg);
						throw new IllegalStateException(msg);
					}
					discover(reference, serviceCollection);
					break;
				case UNREGISTERED:
					String msg = String
							.format("service [%s] has been unregistered after acquisition",
									reference.getUrl());
					logger.info(msg);
					break;
				case MODIFIED:
					logger.info(String.format(
							"service [%s] has been modified after acquisition",
							reference.getUrl()));
					break;
				}
			}
		};
	}

	protected ServiceListener serviceListener() {
		return new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference reference = event.getReference();
				switch (event.getType()) {
				case REGISTERED:
					Service service = services.get(this);
					if (service == null) {
						String msg = String.format(
								"No existing listener matching [%s]",
								reference.getUrl());
						logger.severe(msg);
						throw new IllegalStateException(msg);
					}
					discover(reference, service);
					break;
				case UNREGISTERED:
					logger.info(String
							.format("service [%s] has been unregistered after acquisition",
									reference.getUrl()));
					break;
				case MODIFIED:
					logger.info(String.format(
							"service [%s] has been modified after acquisition",
							reference.getUrl()));
					break;
				}
			}
		};
	}

	protected Runnable successAction(final Runnable success,
			final Runnable failure) {
		return new Runnable() {
			@Override
			public void run() {
				logger.info("All services have been discovered");
				try {
					processConfigurations(getPropertySubstitutions());
				} catch (Throwable e) {
					logger.log(Level.SEVERE, "Error processing configurations",
							e);
					failure.run();
					return;
				}
				logger.info("Auto configuration successfully completed");
				success.run();
			}
		};
	}
}
