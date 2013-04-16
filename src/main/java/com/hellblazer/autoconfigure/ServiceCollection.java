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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceURL;

/**
 * The definition of a collection of services that need to be discovered.
 * 
 * @author hhildebrand
 * 
 */
public class ServiceCollection {
	@JsonProperty
	public int cardinality = 0;
	@JsonProperty
	public String format = "%s:%s";
	@JsonProperty
	public List<String> properties = new ArrayList<>();
	@JsonProperty
	public String separator = ",";
	@JsonProperty
	public String service = "service:someType:someProtocol";
	@JsonProperty
	public Map<String, String> serviceProperties = new HashMap<>();
	@JsonProperty
	public final String variable = "services";
	private List<ServiceReference> discovered = new ArrayList<>();

	public void discover(ServiceReference reference) {
		discovered.add(reference);
	}

	public List<ServiceReference> getDiscovered() {
		return discovered;
	}

	/**
	 * @return the resolved value of this collection
	 */
	public String resolve() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < discovered.size(); i++) {
			ServiceReference service = discovered.get(i);
			ServiceURL url = service.getUrl();
			List<Object> values = new ArrayList<>();
			values.add(url.getHost());
			values.add(url.getPort());
			for (String property : properties) {
				values.add(service.getProperties().get(property));
			}
			builder.append(String.format(format, values.toArray()));
			if (i < discovered.size() - 1) {
				builder.append(separator);
			}
		}
		return builder.toString();
	}

	public String toString() {
		return String.format("Service Collection [%s] [%s] properties %s",
				cardinality, service, serviceProperties);
	}

	/**
	 * @return the query filter for the service collection
	 */
	public String constructFilter() {
		return ConfigureMe.constructFilter(service, serviceProperties);
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
}
