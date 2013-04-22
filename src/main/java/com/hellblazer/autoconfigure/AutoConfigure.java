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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import com.hellblazer.autoconfigure.configuration.Configuration;
import com.hellblazer.autoconfigure.configuration.ConfigurationTemplate;
import com.hellblazer.autoconfigure.configuration.ServiceCollectionDefinition;
import com.hellblazer.autoconfigure.configuration.ServiceDefinition;
import com.hellblazer.autoconfigure.configuration.UniqueDirectory;
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
public class AutoConfigure {
	private static final Logger logger = Logger.getLogger(AutoConfigure.class
			.getCanonicalName());

	public static String constructFilter(String service,
			Map<String, String> properties) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		if (properties.size() != 0) {
			builder.append(" &(");
		}
		builder.append(String.format("%s=%s", ServiceScope.SERVICE_TYPE,
				service));
		if (properties.size() != 0) {
			builder.append(")");
		}
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			builder.append(String.format(" (%s=%s) ", entry.getKey(),
					entry.getValue()));
		}
		builder.append(')');
		return builder.toString();
	}

	private final Map<String, String> additionalPorts = new HashMap<>();
	private final int addressIndex;
	private final AtomicReference<InetSocketAddress> bound = new AtomicReference<>();
	private final ServiceScope discovery;
	private final AtomicBoolean failed = new AtomicBoolean();
	private final Map<String, File> generatedConfigurations = new HashMap<>();
	private final String networkInterface;
	private final Map<String, String> registeredServiceProperties = new HashMap<>();
	private final AtomicReference<Rendezvous> rendezvous = new AtomicReference<>();
	private final Map<String, String> environment = new HashMap<>();
	private final Map<ServiceListener, ServiceCollectionDefinition> serviceCollectionDefinitions = new HashMap<>();
	private final Map<ServiceListener, ServiceDefinition> serviceDefinitions = new HashMap<>();
	private final String serviceFormat;
	private final Map<String, String> serviceProperties;
	private final AtomicReference<UUID> serviceRegistration = new AtomicReference<>();
	private final Map<String, String> variables;
	private final List<ConfigurationTemplate> templates;
	private final AtomicReference<ServiceURL> thisService = new AtomicReference<>();
	private final List<UniqueDirectory> uniqueDirectories;
	private final boolean verboseTemplating;

	/**
	 * Construct an instance from the configuration POJO
	 * 
	 * @param config
	 *            - the configuration to use
	 * @throws SocketException
	 *             - if the discovery service cannot be constructed
	 */
	public AutoConfigure(Configuration config) throws SocketException {
		this(config.serviceUrl, config.networkInterface, config.addressIndex,
				config.serviceProperties, new GossipScope(
						config.gossip.construct()).start(), config.services,
				config.serviceCollections, config.templates, config.variables,
				config.uniqueDirectories, config.additionalPorts,
				config.verboseTemplating);
	}

	/**
	 * Construct an instance
	 * 
	 * @param serviceFormat
	 *            - the format string which results in the service registration
	 *            URL
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
	 * @param services
	 *            - the list of singular services that need to be discovered
	 * @param serviceCollections
	 *            - the list of service collections that need to be discovered
	 * @param variables
	 *            - an additional list of properties that will be substituted in
	 *            the configuration files
	 * @param uniqueDirectories
	 *            - a list of unique directories that will be created and used
	 *            when processing the configurations
	 * @param additionalPorts
	 *            - a list of property names that will be assigned free ports.
	 *            These properties will be added to this instance's service
	 *            registration as well as being used in the processing of the
	 *            configurations.
	 * @param templateGroups
	 *            - the Map of files containing templateGroups that will
	 *            generate the configuration files. The key is the template
	 *            group file, the value is the generated configuration file from
	 *            the template group generates it
	 * @param verboseTemplating
	 *            - if true, turn on verbose processing when processing
	 *            templates
	 */
	public AutoConfigure(String serviceFormat, String networkInterface,
			int addressIndex, Map<String, String> serviceProperties,
			ServiceScope discovery, List<ServiceDefinition> services,
			List<ServiceCollectionDefinition> serviceCollections,
			List<ConfigurationTemplate> templates,
			Map<String, String> variables,
			List<UniqueDirectory> uniqueDirectories,
			List<String> additionalPorts, boolean verboseTemplating) {
		this.serviceFormat = serviceFormat;
		this.networkInterface = networkInterface;
		this.addressIndex = addressIndex;
		this.serviceProperties = serviceProperties;
		this.discovery = discovery;
		this.templates = templates;
		this.variables = variables;
		this.uniqueDirectories = uniqueDirectories;
		this.verboseTemplating = verboseTemplating;

		for (ServiceDefinition service : services) {
			serviceDefinitions.put(serviceListener(), service);
		}
		for (ServiceCollectionDefinition collection : serviceCollections) {
			serviceCollectionDefinitions.put(serviceCollectionListener(),
					collection);
		}
		for (String p : additionalPorts) {
			this.additionalPorts.put(p, p);
		}
		for (ConfigurationTemplate template : templates) {
			generatedConfigurations.put(template.name, template.generated);
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
	public void configure(ConfigurationAction success,
			ConfigurationAction failure, long timeout, TimeUnit unit) {
		configure(new HashMap<String, String>(), success, failure, timeout,
				unit);
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
	 * @param environment
	 *            - a map of variables that override any configured variables
	 */
	public void configure(Map<String, String> environment,
			ConfigurationAction success, ConfigurationAction failure,
			long timeout, TimeUnit unit) {
		this.environment.putAll(environment);
		logger.info(String.format("Using runtime property overrides %s",
				environment));
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
			failed.set(true);
			failure.run(generatedConfigurations);
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
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Error registering service listeners", e);
			failed.set(true);
			rendezvous.get().cancel();
			failure.run(generatedConfigurations);
		}
	}

	/**
	 * Allocate any additional ports required by the configured service
	 * instance.
	 */
	protected void allocateAdditionalPorts() {
		for (Map.Entry<String, String> entry : additionalPorts.entrySet()) {
			entry.setValue(String.valueOf(Utils.allocatePort(bound.get()
					.getAddress())));
		}
	}

	/**
	 * Allocate the main service port, used when registering the instance of the
	 * service being configured.
	 */
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
					"Unable to determine if network interface [%s] is up",
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

	/**
	 * Discover a new instance of the service collection
	 * 
	 * @param reference
	 *            - the service reference of the new instance
	 * @param serviceCollection
	 *            - the service collection definition
	 */
	protected void discover(ServiceReference reference,
			ServiceCollectionDefinition serviceCollection) {
		logger.info(String.format(
				"discovered [%s, %s] for service collection [%s]",
				reference.getUrl(), reference.getProperties(),
				serviceCollection));
		serviceCollection.discover(reference);
		try {
			rendezvous.get().meet();
		} catch (BrokenBarrierException e) {
			logger.finest("Barrier already broken");
		}
	}

	/**
	 * discover a new service singleton
	 * 
	 * @param reference
	 *            - the service reference of the singleton
	 * @param service
	 *            - the service singleton definition
	 */
	protected void discover(ServiceReference reference,
			ServiceDefinition service) {
		logger.info(String.format("discovered [%s, %s] for service [%s]",
				reference.getUrl(), reference.getProperties(), service));
		if (service.isDiscovered()) {
			logger.warning(String.format(
					"Service [%s] has already been discovered!", service));
			return;
		}
		service.discover(reference);
		try {
			rendezvous.get().meet();
		} catch (BrokenBarrierException e) {
			logger.finest("Barrier already broken");
		}
	}

	/**
	 * @param failure
	 * 
	 * @return the action to run on failure
	 */
	protected Runnable failureAction(final ConfigurationAction failure) {
		return new Runnable() {
			@Override
			public void run() {
				if (!failed.compareAndSet(false, true)) {
					return;
				}
				logger.severe("Auto configuration failed due to not all services being discovered");
				for (ServiceDefinition service : serviceDefinitions.values()) {
					if (!service.isDiscovered()) {
						logger.severe(String
								.format("Service [%s] has not been discovered",
										service));
					}
				}
				for (ServiceCollectionDefinition serviceCollection : serviceCollectionDefinitions
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
				failure.run(generatedConfigurations);
			}
		};
	}

	/**
	 * @return the mapping of substitution variables used by the templates
	 */
	protected Map<String, Object> resolveVariables() {
		Map<String, Object> resolvedVariables = new HashMap<>();

		// Add any configured variables
		resolvedVariables.putAll(variables);

		// Add the generated directories
		for (UniqueDirectory uDir : uniqueDirectories) {
			try {
				resolvedVariables.put(uDir.variable, uDir.resolve());
			} catch (IOException e) {
				String msg = String.format(
						"Cannot create unique directory [%s]", uDir);
				logger.log(Level.SEVERE, msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		// Register the service variables
		for (ServiceDefinition definition : serviceDefinitions.values()) {
			resolvedVariables.put(definition.variable,
					definition.constructService());
		}

		// Register the service collection variables
		for (ServiceCollectionDefinition definition : serviceCollectionDefinitions
				.values()) {
			resolvedVariables.put(definition.variable,
					definition.constructServices());
		}

		// Finally, add any property overrides that were specified during the
		// runtime call to configure.
		resolvedVariables.putAll(environment);

		logger.info(String.format("Using property substitions [%s]",
				resolvedVariables));
		return resolvedVariables;
	}

	/**
	 * Generate the configuration file from the template group
	 * 
	 * @param template
	 *            - the template used to generate the configuration
	 * @param thisService
	 *            - The model for the configured service
	 * @param variables
	 *            - the variables used by the template
	 */
	protected void generate(ConfigurationTemplate template,
			Service thisService, Map<String, Object> variables) {
		STGroupFile group = new STGroupFile(
				template.templateGroup.getAbsolutePath());
		STGroupFile.verbose = verboseTemplating;
		STGroupFile.trackCreationEvents = verboseTemplating;
		group.registerModelAdaptor(Service.class, new ServiceModelAdaptor());
		ST st = group.getInstanceOf(template.template);
		if (st == null) {
			String msg = String
					.format("Cannot retrieve template [%s] from template group file [%s]",
							template.template,
							template.templateGroup.getAbsolutePath());
			logger.log(Level.SEVERE, msg);
			throw new IllegalStateException(msg);
		}
		// Register the substitution variables
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			try {
				st.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// Really? This is how I have to detect that there isn't a
				// formal parameter? #fail
			}
		}

		// Finally, register the service being configured
		try {
			st.add(template.thisServiceName, thisService);
		} catch (IllegalArgumentException e) {
			// Really? This is how I have to detect that there isn't a formal
			// parameter? #fail
		}

		// Render!
		String generated = st.render();

		try (Writer writer = new FileWriter(template.generated)) {
			writer.write(generated);
		} catch (IOException e) {
			String msg = String
					.format("Cannot write generated configuration file[%s] for templateGroup [%s]",
							template.generated.getAbsolutePath(),
							template.templateGroup.getAbsolutePath());
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalStateException(msg, e);
		}
	}

	/**
	 * Generate the configuration files from the templates
	 */
	protected void generateConfigurations() {
		Service model = new Service(thisService.get(),
				registeredServiceProperties);
		Map<String, Object> variables = resolveVariables();
		for (ConfigurationTemplate template : templates) {
			if (!template.templateGroup.exists()) {
				String msg = String.format("missing template group file [%s]",
						template.templateGroup.getAbsolutePath());
				logger.severe(msg);
				throw new IllegalStateException(msg);
			}
			generate(template, model, variables);
		}
	}

	/**
	 * Used for testing.
	 * 
	 * @return the primary bound socket address and port for the configured
	 *         service instance.
	 */
	protected InetSocketAddress getBound() {
		return bound.get();
	}

	/**
	 * @return the cardinality of the expected number of services required to
	 *         configure this service instance
	 */
	protected int getCardinality() {
		int cardinality = 0;
		cardinality += serviceDefinitions.size();
		for (ServiceCollectionDefinition collection : serviceCollectionDefinitions
				.values()) {
			cardinality += collection.cardinality;
		}
		logger.info(String.format("Expecting %s service registrations",
				cardinality));
		return cardinality;
	}

	/**
	 * Register the listeners for the required services on the discovery scope
	 */
	protected void registerListeners() {
		registerServiceCollectionListeners();
		registerServiceListeners();
	}

	/**
	 * Register the configured service instance in the discovery scope.
	 */
	protected void registerService() {
		allocatePort();
		allocateAdditionalPorts();
		String service = String.format(serviceFormat,
				bound.get().getHostName(), bound.get().getPort());
		translateServiceProperties();
		registeredServiceProperties.putAll(serviceProperties);
		registeredServiceProperties.putAll(additionalPorts);
		try {
			thisService.set(new ServiceURL(service));
			logger.info(String.format(
					"Registering this service as [%s] with properties %s",
					thisService.get(), registeredServiceProperties));
			serviceRegistration.set(discovery.register(thisService.get(),
					registeredServiceProperties));
		} catch (MalformedURLException e) {
			String msg = String.format("Invalid syntax for service URL [%s]",
					service);
			logger.log(Level.SEVERE, msg, e);
			throw new IllegalArgumentException(msg, e);
		}
	}

	/**
	 * Register the listeners for the required service collections on the
	 * discovery scope
	 */
	protected void registerServiceCollectionListeners() {
		for (Map.Entry<ServiceListener, ServiceCollectionDefinition> entry : serviceCollectionDefinitions
				.entrySet()) {
			ServiceCollectionDefinition service = entry.getValue();
			try {
				logger.info(String.format(
						"Registering listener for service collection %s",
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

	/**
	 * Register the listeners for the required singleton services on the
	 * discovery scope
	 */
	protected void registerServiceListeners() {
		for (Map.Entry<ServiceListener, ServiceDefinition> entry : serviceDefinitions
				.entrySet()) {
			ServiceDefinition service = entry.getValue();
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

	/**
	 * @return a service collection listener
	 */
	protected ServiceListener serviceCollectionListener() {
		return new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference reference = event.getReference();
				switch (event.getType()) {
				case REGISTERED:
					ServiceCollectionDefinition serviceCollection = serviceCollectionDefinitions
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

	/**
	 * @return a listener for a singleton service
	 */
	protected ServiceListener serviceListener() {
		return new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference reference = event.getReference();
				if (reference.getRegistration().equals(
						serviceRegistration.get())) {
					logger.finest(String
							.format("Ignoring service event for this instance's service"));
					return;
				}
				switch (event.getType()) {
				case REGISTERED:
					ServiceDefinition service = serviceDefinitions.get(this);
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

	/**
	 * @param success
	 * @param failure
	 * @return the action to run upon successful configuration
	 */
	protected Runnable successAction(final ConfigurationAction success,
			final ConfigurationAction failure) {
		return new Runnable() {
			@Override
			public void run() {
				if (failed.get()) {
					return;
				}
				logger.info("All services have been discovered");
				try {
					generateConfigurations();
				} catch (Throwable e) {
					logger.log(Level.SEVERE, "Error processing configurations",
							e);
					failed.set(true);
					failure.run(generatedConfigurations);
					return;
				}
				logger.info("Auto configuration successfully completed, running success action");
				try {
					success.run(generatedConfigurations);
					logger.info("Success action completed");
				} catch (Throwable e) {
					logger.log(
							Level.SEVERE,
							"Exception encountered during the running success action",
							e);
					failed.set(true);
					logger.info("Running failure action");
					failure.run(generatedConfigurations);
				}
			}
		};
	}

	/**
	 * Translate any service properties by replacing their value with the value
	 * from either the environment or the configured variables
	 */
	protected void translateServiceProperties() {
		for (Map.Entry<String, String> entry : serviceProperties.entrySet()) {
			if (entry.getValue().startsWith("$")) {
				// Find a replacement value
				String replacement = entry.getValue().substring(1);
				String translated = environment.get(replacement);
				if (translated == null) {
					translated = variables.get(replacement);
				}
				if (translated == null) {
					String msg = String
							.format("Could not find a substitution for service property: %s, replacement variable: %s",
									entry.getKey(), replacement);
					logger.log(Level.SEVERE, msg);
					throw new IllegalArgumentException(msg);
				}
				logger.info(String
						.format("Replacing service property: %s with translated value: %s [property: %s]",
								entry.getKey(), translated, replacement));
				entry.setValue(translated);
			}
		}
	}
}
