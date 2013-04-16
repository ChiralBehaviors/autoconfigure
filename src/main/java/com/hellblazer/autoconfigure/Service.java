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
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;

/**
 * The definition of a singluar service that needs to be discovered
 * 
 * @author hhildebrand
 * 
 */
public class Service {
	private volatile ServiceReference discovered;

	@JsonProperty
	public String service = "service:someType:someProtocol";
	@JsonProperty
	public String format = "%s:%s";
	@JsonProperty
	public String variable = "service";
	@JsonProperty
	public List<String> properties = new ArrayList<>();
	@JsonProperty
	public Map<String, String> serviceProperties = new HashMap<>();

	public ServiceReference getDiscovered() {
		return discovered;
	}

	public void discover(ServiceReference discovered) {
		this.discovered = discovered;
	}

	/**
	 * @return the resolved value of this service
	 */
	public String resolve() {
		ServiceURL url = discovered.getUrl();
		List<Object> values = new ArrayList<>();
		values.add(url.getHost());
		values.add(url.getPort());
		for (String property : properties) {
			values.add(discovered.getProperties().get(property));
		}
		return String.format(format, values.toArray());
	}

	public String toString() {
		return String.format("Service [%s] properties [%s]", service,
				serviceProperties);
	}

	/**
	 * @return the query filter for the service collection
	 */
	public String constructFilter() {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		if (serviceProperties.size() != 0) {
			builder.append(" &(");
		}
		builder.append(String.format("%s=%s",
				ServiceScope.SERVICE_REGISTRATION, service));
		if (serviceProperties.size() != 0) {
			builder.append(")");
		}
		for (Map.Entry<String, String> entry : serviceProperties.entrySet()) {
			builder.append(String.format(" (%s=%s) ", entry.getKey(),
					entry.getValue()));
		}
		builder.append(')');
		return builder.toString();
	}

	/**
	 * @return true if the service has been discovered
	 */
	public boolean isDiscovered() {
		return discovered != null;
	}
}
