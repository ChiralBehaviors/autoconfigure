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
package com.hellblazer.autoconfigure.jmx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;

import com.hellblazer.autoconfigure.configuration.JmxConfiguration;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceType;
import com.hellblazer.slp.ServiceURL;
import com.sun.jmx.remote.internal.RMIExporter;

@SuppressWarnings("restriction")
public class JmxDiscovery {
    private static class Exporter implements RMIExporter {
	/**
	 * <p>
	 * Prevents our RMI server objects from keeping the JVM alive.
	 * </p>
	 * 
	 * <p>
	 * We use a private interface in Sun's JMX Remote API implementation
	 * that allows us to specify how to export RMI objects. We do so using
	 * UnicastServerRef, a class in Sun's RMI implementation. This is all
	 * non-portable, of course, so this is only valid because we are inside
	 * Sun's JRE.
	 * </p>
	 * 
	 * <p>
	 * Objects are exported using
	 * {@link UnicastServerRef#exportObject(Remote, Object, boolean)}. The
	 * boolean parameter is called <code>permanent</code> and means both
	 * that the object is not eligible for Distributed Garbage Collection,
	 * and that its continued existence will not prevent the JVM from
	 * exiting. It is the latter semantics we want (we already have the
	 * former because of the way the JMX Remote API works). Hence the
	 * somewhat misleading name of this class.
	 * </p>
	 */

	@Override
	public Remote exportObject(Remote obj, int port,
		RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
		throws RemoteException {
	    final UnicastServerRef ref;
	    if (csf == null && ssf == null) {
		ref = new UnicastServerRef(port);
	    } else {
		ref = new UnicastServerRef2(port, csf, ssf);
	    }
	    return ref.exportObject(obj, null, true);
	}

	// Nothing special to be done for this case
	@Override
	public boolean unexportObject(Remote obj, boolean force)
		throws NoSuchObjectException {
	    return UnicastRemoteObject.unexportObject(obj, force);
	}
    }

    private static final Logger log = LoggerFactory
	    .getLogger(JmxDiscovery.class);

    public static JMXConnectorServer contruct(InetSocketAddress jmxEndpoint,
	    MBeanServer mbs) throws IOException {

	// Ensure cryptographically strong random number generater used
	// to choose the object number - see java.rmi.server.ObjID
	System.setProperty("java.rmi.server.randomIDs", "true");

	// This RMI server should not keep the VM alive
	Map<String, RMIExporter> env = new HashMap<String, RMIExporter>();
	env.put(RMIExporter.EXPORTER_ATTRIBUTE, new Exporter());
	JMXServiceURL url = new JMXServiceURL("rmi", jmxEndpoint.getHostName(),
		jmxEndpoint.getPort());
	return JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
    }

    private UUID registration;
    private final AtomicBoolean running = new AtomicBoolean();
    private ServiceScope scope;
    private JMXConnectorServer server;

    private String serviceType;

    public JmxDiscovery(JmxConfiguration configuration, ServiceScope scope) {

    }

    public void shutdown() throws IOException {
	if (running.compareAndSet(true, false)) {
	    scope.unregister(registration);
	    server.stop();
	}
    }

    public void start() throws IOException {
	if (running.compareAndSet(false, true)) {
	    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		@Override
		public void run() {
		    try {
			shutdown();
		    } catch (IOException e) {
			log.trace("Error shutting down", e);
		    }
		}
	    }, "Jmx Discovery Shutdown Hook"));
	    server.start();
	    ServiceURL serviceUrl = constructServiceURL(server.getAddress());
	    log.info(String.format("Registering as %s", serviceUrl));
	    registration = scope.register(serviceUrl,
		    new HashMap<String, String>());
	}
    }

    protected ServiceURL constructServiceURL(JMXServiceURL url)
	    throws MalformedURLException {
	StringBuilder builder = new StringBuilder();
	builder.append(ServiceType.SERVICE_PREFIX);
	builder.append(serviceType);
	builder.append(':');
	builder.append("jmx:");
	builder.append(url.getProtocol());
	builder.append("://");
	builder.append(url.getHost());
	builder.append(':');
	builder.append(url.getPort());
	builder.append(url.getURLPath());
	ServiceURL jmxServiceURL = new ServiceURL(builder.toString());
	return jmxServiceURL;
    }
}
