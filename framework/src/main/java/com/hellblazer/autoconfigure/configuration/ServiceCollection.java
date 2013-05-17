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
package com.hellblazer.autoconfigure.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hellblazer.autoconfigure.AutoConfigure;
import com.hellblazer.autoconfigure.Cluster;
import com.hellblazer.autoconfigure.ClusterImpl;
import com.hellblazer.autoconfigure.Service;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceScope;

/**
 * The definition of a collection of services that need to be discovered.
 * 
 * @author hhildebrand
 * 
 */
public class ServiceCollection {
	public int cardinality = 0;
	public String idProperty = "totalOrderingIndex";
	public Map<String, String> properties = new HashMap<>();
	public String service;
	public String variable;
	private List<Service> discovered = new ArrayList<>();

	/**
	 * @return the query filter for the service collection
	 */
	public String constructFilter() {
		return AutoConfigure.constructFilter(service, properties);
	}

	/**
	 * @return the Cluster of service models discovered for this collection
	 */
	public Cluster getCluster() {
		return new ClusterImpl(discovered);
	}

	public void discover(ServiceReference reference) {
		discovered.add(new Service(reference.getUrl(), reference
				.getProperties()));
		canonicalizeServices();
	}

	/**
	 * @return the number of services discovered for this collection
	 */
	public int getDiscoveredCardinality() {
		return discovered.size();
	}

	/**
	 * Answer the index of the service registered with the uuid in the total
	 * ordering of the receiver's services
	 * 
	 * @param uuid
	 * @return the String representing the index of this service, or null if not
	 *         found
	 */
	public String totalOrderingIndexOf(UUID uuid) {
		String registration = uuid.toString();
		for (Service service : discovered) {
			if (registration.equals(service.getProperties().get(
					ServiceScope.SERVICE_REGISTRATION))) {
				return service.getProperties().get(idProperty);
			}
		}
		return null;
	}

	/**
	 * @return true if all the services have been discovered
	 */
	public boolean isSatisfied() {
		return discovered.size() == cardinality;
	}

	@Override
	public String toString() {
		return String.format("Service Collection [%s] [%s] properties %s",
				cardinality, service, properties);
	}

	/**
	 * Canonicalize the services, providing a total ordering of the services.
	 * Add the unique index of each service to its properties, using the
	 * supplied idProperty as the property key
	 */
	protected void canonicalizeServices() {
		Collections.sort(discovered);
		for (int i = 1; i <= discovered.size(); i++) {
			discovered.get(i - 1).getProperties()
					.put(idProperty, String.valueOf(i));
		}
	}
}
