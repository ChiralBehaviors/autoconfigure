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
package com.hellblazer.autoconfigure.debug;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hellblazer.autoconfigure.Service;
import com.hellblazer.autoconfigure.ServiceModelAdaptor;
import com.hellblazer.autoconfigure.configuration.Template;

/**
 * A simple driver you can use to debug your templates without requiring the
 * entire effing configuration process to be run.
 * 
 * @author hhildebrand
 * 
 */
public class TemplateDebugger {

	public static void main(String[] argv) throws JsonParseException,
			JsonMappingException, IOException {
		if (argv.length == 0) {
			System.out.println("Usage: TemplateDebugger <scenario file>+");
		}
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		for (String fileName : argv) {
			FileInputStream yaml = new FileInputStream(fileName);
			TemplateDebugger debugger = mapper.readValue(yaml,
					TemplateDebugger.class);
			System.out.println("======================================");
			System.out.println(String.format("Rendered output of %s", yaml));
			System.out.println("======================================");
			System.out.println();
			System.out.println(debugger.render());
			System.out.println();
			System.out.println("======================================");
			System.out.println();
			System.out.println();
		}
	}

	@JsonProperty
	private final Map<String, List<Map<String, String>>> serviceCollections = new HashMap<>();
	@JsonProperty
	private final Map<String, Map<String, String>> services = new HashMap<>();
	@JsonProperty
	private String templateGroupFile;
	@JsonProperty
	private String templateName = Template.CONFIGURATION;
	@JsonProperty
	private Map<String, String> variables = new HashMap<>();

	/**
	 * Render the named template in the template group, using the state of this
	 * instance.
	 */
	public String render() {
		STGroupFile templateGroup = new STGroupFile(templateGroupFile);
		templateGroup.registerModelAdaptor(Service.class,
				new ServiceModelAdaptor());
		ST template = templateGroup.getInstanceOf(templateName);
		if (template == null) {
			throw new IllegalStateException(
					String.format(
							"The template named [%s] does not exist in the template group [%s]",
							templateName, templateGroupFile));
		}
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		for (Map.Entry<String, Map<String, String>> entry : services.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		for (Entry<String, List<Map<String, String>>> entry : serviceCollections
				.entrySet()) {
			try {
				template.add(entry.getKey(), entry.getValue());
			} catch (IllegalArgumentException e) {
				// no parameter to this template
			}
		}
		return template.render();
	}
}
