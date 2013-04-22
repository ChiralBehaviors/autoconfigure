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

import java.util.HashMap;
import java.util.Map;

import com.hellblazer.slp.ServiceURL;

/**
 * The model for discovered services
 * 
 * @author hhildebrand
 * 
 */
public class Service {
	private final Map<String, String> properties = new HashMap<>();
	private final ServiceURL serviceUrl;

	public Service(ServiceURL serviceUrl, Map<String, String> serviceProperties) {
		this.serviceUrl = serviceUrl;
		properties.putAll(serviceProperties);
	}

	/**
	 * A convienence method to return the host of the service instance.
	 * 
	 * @return host of this service's URL
	 */
	public String getHost() {
		return serviceUrl.getHost();
	}

	/**
	 * A convienence method to return the port of the service instance.
	 * 
	 * @return port of this service's URL
	 */
	public int getPort() {
		return serviceUrl.getPort();
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public ServiceURL getServiceUrl() {
		return serviceUrl;
	}
	public String toString() {
		return String.format("Service[%s] properties: %s", serviceUrl,
				properties);
	}
}
