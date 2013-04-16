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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceReference;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;
import com.hellblazer.utils.Condition;
import com.hellblazer.utils.Utils;

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
				"port", "en0", 0, serviceProperties, discovery,
				serviceDefinitions, serviceCollectionDefinitions,
				configurations, substitutions);
		final AtomicBoolean succeeded = new AtomicBoolean();
		final AtomicBoolean completed = new AtomicBoolean();
		Runnable success = new Runnable() {
			@Override
			public void run() {
				succeeded.set(true);
				completed.set(true);
			}
		};
		Runnable failure = new Runnable() {
			@Override
			public void run() {
				succeeded.set(false);
				completed.set(true);
			}
		};
		configureMe.configure(success, failure, 10, TimeUnit.MILLISECONDS);
		assertTrue("configuration did not complete",
				Utils.waitForCondition(1000, new Condition() {
					@Override
					public boolean isTrue() {
						return completed.get();
					}
				}));
		assertTrue("configuration did not succeed", succeeded.get());
	}

	@Test
	public void testServiceRegistration() throws Exception {
		String serviceFormat = "service:test:tcp://%s:%s";
		Map<String, String> serviceProperties = new HashMap<String, String>();
		Service service = new Service();
		service.service = "service:testService:tcp";
		List<Service> serviceDefinitions = new ArrayList<>();
		serviceDefinitions.add(service);
		ServiceCollection serviceCollection = new ServiceCollection();
		serviceCollection.service = "service:testServiceCollection:tcp";
		serviceCollection.cardinality = 1;
		List<ServiceCollection> serviceCollectionDefinitions = new ArrayList<>();
		serviceCollectionDefinitions.add(serviceCollection);
		List<File> configurations = new ArrayList<>();
		Map<String, String> substitutions = new HashMap<>();
		ConfigureMe configureMe = new ConfigureMe(serviceFormat, "host",
				"port", "en0", 0, serviceProperties, discovery,
				serviceDefinitions, serviceCollectionDefinitions,
				configurations, substitutions);
		final AtomicBoolean completed = new AtomicBoolean();
		final AtomicBoolean succeeded = new AtomicBoolean();
		Runnable success = new Runnable() {
			@Override
			public void run() {
				succeeded.set(true);
				completed.set(true);
			}
		};
		Runnable failure = new Runnable() {
			@Override
			public void run() {
				succeeded.set(false);
				completed.set(true);
			}
		};
		configureMe.configure(success, failure, 100, TimeUnit.MILLISECONDS);
		ServiceReference serviceRef = mock(ServiceReference.class);
		ServiceURL serviceUrl = mock(ServiceURL.class);
		when(serviceRef.getUrl()).thenReturn(serviceUrl);
		when(serviceUrl.getHost()).thenReturn("example.com");
		when(serviceUrl.getPort()).thenReturn(1);
		configureMe.discover(serviceRef, service);
		ServiceReference serviceCollectionRef = mock(ServiceReference.class);
		ServiceURL serviceCollectionUrl = mock(ServiceURL.class);
		when(serviceCollectionUrl.getHost()).thenReturn("example.com");
		when(serviceCollectionUrl.getPort()).thenReturn(2);
		when(serviceCollectionRef.getUrl()).thenReturn(serviceCollectionUrl);
		configureMe.discover(serviceCollectionRef, serviceCollection);
		assertTrue("configuration did not complete",
				Utils.waitForCondition(1000, new Condition() {
					@Override
					public boolean isTrue() {
						return completed.get();
					}
				}));
		assertTrue("configuration not successful", succeeded.get());
		verify(discovery).addServiceListener(
				isA(ServiceListener.class),
				eq(ConfigureMe.constructFilter(service.service,
						service.serviceProperties)));
		verify(discovery).addServiceListener(
				isA(ServiceListener.class),
				eq(ConfigureMe.constructFilter(serviceCollection.service,
						serviceCollection.serviceProperties)));
	}
}
