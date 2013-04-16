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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hellblazer.slp.ServiceScope;
import static org.junit.Assert.*;

/**
 * @author hhildebrand
 * 
 */
public class TestConfigureMe {

	@Mock
	private ServiceScope discovery;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testNoServicesRequired() {
		String serviceFormat = "service:test:tcp://%s:%s";
		Map<String, String> serviceProperties = new HashMap<String, String>();
		List<Service> serviceDefinitions = new ArrayList<>();
		List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
		List<File> configurations = new ArrayList<>();
		Map<String, String> substitutions = new HashMap<>();
		ConfigureMe configureMe = new ConfigureMe(serviceFormat, "host",
				"port", "en0", serviceProperties, discovery,
				serviceDefinitions, serviceCollectionDefinitions,
				configurations, substitutions);
		final AtomicBoolean succeeded = new AtomicBoolean();
		Runnable success = new Runnable() {
			@Override
			public void run() {
				succeeded.set(true);
			}
		};
		Runnable failure = new Runnable() {
			@Override
			public void run() {
				succeeded.set(false);
			}
		};
		configureMe.configure(success, failure, 10, TimeUnit.MILLISECONDS);
		assertTrue(succeeded.get());
	}
}
