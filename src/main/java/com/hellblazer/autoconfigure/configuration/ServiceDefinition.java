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

import java.util.HashMap;
import java.util.Map;

import com.hellblazer.autoconfigure.AutoConfigure;
import com.hellblazer.autoconfigure.Service;
import com.hellblazer.slp.ServiceReference;

/**
 * The definition of a singluar service that needs to be discovered
 * 
 * @author hhildebrand
 * 
 */
public class ServiceDefinition {
	private volatile ServiceReference discovered;

	public String service = "service:someType:someProtocol";
	public String variable = "service";
	public Map<String, String> properties = new HashMap<>();

	public ServiceReference getDiscovered() {
		return discovered;
	}

	public void discover(ServiceReference discovered) {
		this.discovered = discovered;
	}

	public String toString() {
		return String.format("Service [%s] properties %s", service, properties);
	}

	/**
	 * @return the query filter for the service collection
	 */
	public String constructFilter() {
		return AutoConfigure.constructFilter(service, properties);
	}

	/**
	 * @return true if the service has been discovered
	 */
	public boolean isDiscovered() {
		return discovered != null;
	}

	/**
	 * @return the service model discovered for this singleton
	 */
	public Service constructService() {
		return new Service(discovered.getUrl(), discovered.getProperties());
	}
}
