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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hellblazer.autoconfigure.AutoConfigure;
import com.hellblazer.autoconfigure.model.Service;
import com.hellblazer.slp.ServiceReference;

/**
 * The definition of a collection of services that need to be discovered.
 * 
 * @author hhildebrand
 * 
 */
public class ServiceCollectionDefinition {
	public int cardinality = 0;
	public String service = "service:someType:someProtocol";
	public Map<String, String> properties = new HashMap<>();
	public String variable = "services";
	private List<ServiceReference> discovered = new ArrayList<>();

	public void discover(ServiceReference reference) {
		discovered.add(reference);
	}

	public List<ServiceReference> getDiscovered() {
		return discovered;
	}

	public String toString() {
		return String.format("Service Collection [%s] [%s] properties %s",
				cardinality, service, properties);
	}

	/**
	 * @return the query filter for the service collection
	 */
	public String constructFilter() {
		return AutoConfigure.constructFilter(service, properties);
	}

	/**
	 * @return true if all the services have been discovered
	 */
	public boolean isSatisfied() {
		return discovered.size() == cardinality;
	}

	/**
	 * @return the number of services discovered for this collection
	 */
	public int getDiscoveredCardinality() {
		return discovered.size();
	}

	/**
	 * @return
	 */
	public List<Service> constructServices() {
		// TODO Auto-generated method stub
		return null;
	}
}
