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
package com.hellblazer.autoconfigure.util;

import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.ST;

import com.hellblazer.autoconfigure.Service;
import com.hellblazer.autoconfigure.configuration.Template;

/**
 * A simple driver you can use to debug your templates without requiring the
 * entire effing configuration process to be run.
 * 
 * @author hhildebrand
 * 
 */
public class TemplateDebugger {

	private final String configuredServiceVariable;
	private final Service configuredService;
	private final Map<String, Service> singletonServices;
	private final Map<String, List<Service>> serviceCollections;
	private final Map<String, String> variables;

	public TemplateDebugger(Service configuredService,
			Map<String, Service> singletonServices,
			Map<String, List<Service>> serviceCollections,
			Map<String, String> variables) {
		this(Template.CONFIGURATION, configuredService, singletonServices,
				serviceCollections, variables);
	}

	public TemplateDebugger(String configuredServiceVariable,
			Service configuredService, Map<String, Service> singletonServices,
			Map<String, List<Service>> serviceCollections,
			Map<String, String> variables) {
		this.configuredServiceVariable = configuredServiceVariable;
		this.configuredService = configuredService;
		this.singletonServices = singletonServices;
		this.serviceCollections = serviceCollections;
		this.variables = variables;
	}

	public String render(ST template) {
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		for (Map.Entry<String, Service> entry : singletonServices.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		for (Map.Entry<String, List<Service>> entry : serviceCollections
				.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		try {
			template.add(configuredServiceVariable, configuredService);
		} catch (IllegalArgumentException e) {
			// no parameter to this template
		}
		return template.render();
	}
}
