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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.junit.Test;

import com.hellblazer.gossip.Gossip;
import com.hellblazer.gossip.configuration.GossipConfiguration;
import com.hellblazer.utils.Condition;
import com.hellblazer.utils.TemporaryDirectory;
import com.hellblazer.utils.Utils;

/**
 * @author hhildebrand
 * 
 */
public class ZookeeperExample {

	@Test
	public void example() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.FATAL);
		Gossip gossipSeed = new GossipConfiguration().construct();
		InetSocketAddress gossipSeedAddress = gossipSeed.getLocalAddress();
		gossipSeed.start();
		final ZookeeperLauncher launcher1 = new ZookeeperLauncher();
		final ZookeeperLauncher launcher2 = new ZookeeperLauncher();

		try (TemporaryDirectory dir1 = new TemporaryDirectory(
				"functional-test-1-", ".dir", new File("").getAbsoluteFile());
				TemporaryDirectory dir2 = new TemporaryDirectory(
						"functional-test-2-", ".dir",
						new File("").getAbsoluteFile());) {
			final File autoconfig1 = new File(dir1.directory,
					"autoconfigure.yml").getAbsoluteFile();
			final File autoconfig2 = new File(dir2.directory,
					"autoconfigure.yml").getAbsoluteFile();
			initializeDirectories(gossipSeedAddress, dir1.directory,
					dir2.directory, autoconfig1, autoconfig2);
			Thread daemon1 = new Thread(new Runnable() {
				public void run() {
					try {
						launcher1.launch("1", autoconfig1);
					} catch (IOException e) {
						throw new IllegalStateException(
								"Unable to configure zookeeper 1", e);
					}
				};
			}, "Zookeeper 1 launcher");
			Thread daemon2 = new Thread(new Runnable() {
				public void run() {
					try {
						launcher2.launch("2", autoconfig2);
					} catch (IOException e) {
						throw new IllegalStateException(
								"Unable to configure zookeeper 2", e);
					}
				};
			}, "Zookeeper 2 launcher");
			daemon1.start();
			daemon2.start();
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

			System.out.println("Waiting for peers to elect a leader");
			assertTrue("Zookeeper 1 did not find its peer",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher1.getQuorumPeer().getPeerState() != ServerState.LOOKING;
						}
					}));

			Thread.sleep(100); // Just because
			assertTrue("Zookeeper 2 did not find its peer",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher2.getQuorumPeer().getPeerState() != ServerState.LOOKING;
						}
					}));

			System.out.println("Verifying peer views in synch");
			Thread.sleep(2000); // Just because

			assertTrue("Zookeeper 1 does not have the correct view size",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher1.getQuorumPeer().getView().size() == 2;
						}
					}));

			Thread.sleep(100); // Just because
			assertTrue("Zookeeper 2 does not have the correct view size",
					Utils.waitForCondition(60 * 1000, new Condition() {
						@Override
						public boolean isTrue() {
							return launcher1.getQuorumPeer().getView().size() == 2;
						}
					}));
			System.out.println("Everything is hunky dory");
		} finally {
			if (launcher1.getQuorumPeer() != null) {
				launcher1.getQuorumPeer().shutdown();
			}
			if (launcher2.getQuorumPeer() != null) {
				launcher2.getQuorumPeer().shutdown();
			}
		}
	}

	private void initializeDirectories(InetSocketAddress gossipSeedAddress,
			File dir1, File dir2, File autoconfig1, File autoconfig2)
			throws IOException {
		File autoConfigOrig = new File(
				"src/test/resources/zookeeper/autoconfigure.yml");
		File templateOrg = new File("src/test/resources/zookeeper/zoo.stg");
		File template1 = new File(dir1, "zookeeper.stg").getAbsoluteFile();
		File template2 = new File(dir2, "zookeeper.stg").getAbsoluteFile();
		Map<String, String> testProperties = new HashMap<>();

		// set up bootstrap properties, used to set up the initial
		// configuration
		testProperties.put("gossip.seed.host", gossipSeedAddress.getHostName());
		testProperties.put("gossip.seed.port",
				String.valueOf(gossipSeedAddress.getPort()));
		testProperties.put("network.interface", NetworkInterface.getByIndex(1)
				.getName());
		testProperties.put("test.dir", dir1.getAbsolutePath());

		// copy and transform our test configurations
		Utils.replaceProperties(autoConfigOrig, autoconfig1, testProperties);
		Utils.replaceProperties(templateOrg, template1, testProperties);

		testProperties.put("test.dir", dir2.getAbsolutePath());
		Utils.replaceProperties(autoConfigOrig, autoconfig2, testProperties);
		Utils.replaceProperties(templateOrg, template2, testProperties);
	}
}
