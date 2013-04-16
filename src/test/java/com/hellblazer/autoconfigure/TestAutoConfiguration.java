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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Test;

import com.hellblazer.gossip.configuration.GossipConfiguration;

/**
 * @author hhildebrand
 * 
 */
public class TestAutoConfiguration {

	@Test
	public void configurationTest() throws Exception {
		InputStream is = getClass().getResourceAsStream(
				"/yaml/autoconfigure.yml");
		AutoConfiguration config = YamlHelper.fromYaml(is);
		assertNotNull(config);
		GossipConfiguration gossip = config.gossip;
		assertNotNull(gossip);
		List<InetSocketAddress> seeds = gossip.seeds;
		assertNotNull(gossip);
		assertEquals(new InetSocketAddress("localhost", 6754), seeds.get(0));
		assertEquals(new InetSocketAddress("localhost", 6543), seeds.get(1));
		List<ServiceCollection> serviceCollections = config.serviceCollections;
		assertNotNull(serviceCollections);
		assertEquals(1, serviceCollections.size());
		ServiceCollection serviceCollection = serviceCollections.get(0);
		assertEquals(5, serviceCollection.cardinality);
		assertEquals("%s!%s", serviceCollection.format);
		assertEquals(":", serviceCollection.separator);
		assertEquals("service:iron:man", serviceCollection.service);
		assertEquals(5, serviceCollection.cardinality);

		List<Service> services = config.services;
		assertNotNull(services);
		assertEquals(1, services.size());
		Service service = services.get(0);
		assertEquals("%s|%s", service.format);
		assertEquals("service:thor:rmi", service.service);
	}
}
