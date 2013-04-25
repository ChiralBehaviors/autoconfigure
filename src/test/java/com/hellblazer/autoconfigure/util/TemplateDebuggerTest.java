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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hellblazer.utils.Utils;

/**
 * @author hhildebrand
 * 
 */
public class TemplateDebuggerTest {

	@Test
	public void testDebugger() throws Exception {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		InputStream yaml = getClass().getResourceAsStream(
				"/yaml/templateDebugger.yml");
		TemplateDebugger debugger = mapper.readValue(yaml,
				TemplateDebugger.class);
		assertNotNull(debugger);
		String rendered = debugger.render();
		assertNotNull(rendered);
		InputStream gold = getClass().getResourceAsStream("/configurations/templateDebugger.rendered");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utils.copy(gold, baos);
		String expected = baos.toString();
		assertEquals(expected, rendered);
	}
}
