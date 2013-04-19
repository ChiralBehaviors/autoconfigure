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
package com.hellblazer.autoconfigure.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hellblazer.slp.ServiceURL;

/**
 * The model of the service instance being configured.
 * 
 * @author hhildebrand
 * 
 */
public class ThisService extends Service {
	private final Map<String, List<Service>> serviceCollections = new HashMap<>();
	private final Map<String, Service> services = new HashMap<>();

	public ThisService(ServiceURL serviceUrl,
			Map<String, String> serviceProperties,
			Map<String, Service> services,
			Map<String, List<Service>> serviceCollections) {
		super(serviceUrl, serviceProperties);
		this.services.putAll(services);
		this.serviceCollections.putAll(serviceCollections);
	}

	/**
	 * @return the mapping of variable names to collections of discovered
	 *         services
	 */
	public Map<String, List<Service>> getServiceCollections() {
		return serviceCollections;
	}

	/**
	 * @return the mapping of variable names to discovered service singletons
	 */
	public Map<String, Service> getServices() {
		return services;
	}
}
