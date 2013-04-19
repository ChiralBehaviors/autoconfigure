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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Test;

import com.hellblazer.autoconfigure.configuration.Configuration;
import com.hellblazer.autoconfigure.configuration.ServiceCollectionDefinition;
import com.hellblazer.autoconfigure.configuration.ServiceDefinition;
import com.hellblazer.autoconfigure.configuration.UniqueDirectory;
import com.hellblazer.gossip.configuration.GossipConfiguration;

/**
 * @author hhildebrand
 * 
 */
public class TestConfiguration {

	@Test
	public void configurationTest() throws Exception {
		InputStream is = getClass().getResourceAsStream(
				"/yaml/autoconfigure.yml");
		Configuration config = YamlHelper.fromYaml(is);
		assertNotNull(config);
		GossipConfiguration gossip = config.gossip;
		assertNotNull(gossip);
		List<InetSocketAddress> seeds = gossip.seeds;
		assertNotNull(gossip);
		assertEquals(new InetSocketAddress("localhost", 6754), seeds.get(0));
		assertEquals(new InetSocketAddress("localhost", 6543), seeds.get(1));
		List<ServiceCollectionDefinition> serviceCollections = config.serviceCollections;
		assertNotNull(serviceCollections);
		assertEquals(1, serviceCollections.size());
		ServiceCollectionDefinition serviceCollection = serviceCollections
				.get(0);
		assertEquals(5, serviceCollection.cardinality);
		assertEquals("service:iron:man", serviceCollection.service);
		assertEquals(5, serviceCollection.cardinality);

		List<ServiceDefinition> services = config.services;
		assertNotNull(services);
		assertEquals(1, services.size());
		ServiceDefinition service = services.get(0);
		assertEquals("service:thor:rmi", service.service);

		List<UniqueDirectory> uniqueDirectories = config.uniqueDirectories;
		assertNotNull(uniqueDirectories);
		assertEquals(1, uniqueDirectories.size());
		UniqueDirectory dir = uniqueDirectories.get(0);
		assertEquals(new File("/tmp"), dir.base);
		assertEquals("log-", dir.prefix);
		assertEquals(".dir", dir.suffix);
		assertEquals("log.directory", dir.variable);
	}
}
