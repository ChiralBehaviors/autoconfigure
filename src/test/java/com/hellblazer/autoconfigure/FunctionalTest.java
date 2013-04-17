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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.hellblazer.gossip.Gossip;
import com.hellblazer.gossip.configuration.GossipConfiguration;
import com.hellblazer.utils.Condition;
import com.hellblazer.utils.TemporaryDirectory;
import com.hellblazer.utils.Utils;
import static org.junit.Assert.*;

/**
 * @author hhildebrand
 * 
 */
public class FunctionalTest {

	// @Test
	public void example() throws Exception {
		try (TemporaryDirectory tempDir = new TemporaryDirectory(
				"functional-test-", ".dir", new File(".").getAbsoluteFile());) {
			BasicConfigurator.configure();
			File autoconfig = new File(tempDir.directory, "autoconfigure.yml");
			Map<String, String> testProperties = new HashMap<>();
			Gossip gossipSeed = new GossipConfiguration().construct();
			InetSocketAddress gossipSeedAddress = gossipSeed.getLocalAddress();
			gossipSeed.start();

			// set up bootstrap properties, used to set up the initial
			// configuration
			testProperties.put("gossip.seed.host",
					gossipSeedAddress.getHostName());
			testProperties.put("gossip.seed.port",
					String.valueOf(gossipSeedAddress.getPort()));
			testProperties.put("test.dir", tempDir.directory.getAbsolutePath());
			// copy and transform our test configuration
			Utils.replaceProperties(new File(
					"src/test/resources/zookeeper/autoconfigure.yml"),
					autoconfig, testProperties);
			Utils.replaceProperties(new File(
					"src/test/resources/zookeeper/zoo.cfg"), new File(
					tempDir.directory, "zookeeper.cfg"), testProperties);
			final ZookeeperLauncher launcher1 = new ZookeeperLauncher();
			final ZookeeperLauncher launcher2 = new ZookeeperLauncher();
			launcher1.launch(autoconfig);
			launcher2.launch(autoconfig);
			assertTrue("Zookeeper 1 did not complete configuration",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher1.configurationCompleted.get();
						}
					}));
			assertTrue("Zookeeper 1 did not launch successfully",
					launcher1.success.get());
			assertTrue("Zookeeper 2 did not complete configuration",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher2.configurationCompleted.get();
						}
					}));
			assertTrue("Zookeeper 2 did not launch successfully",
					launcher2.success.get());
		}
	}
}
