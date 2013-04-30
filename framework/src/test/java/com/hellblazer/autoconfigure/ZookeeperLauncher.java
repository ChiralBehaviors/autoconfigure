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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * An example of using auto configuration to simplify the configuration of a
 * zookeeper cluster.
 * 
 * @author hhildebrand
 * 
 */
public class ZookeeperLauncher extends AutoConfigureService {
	/**
	 * @param string
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public ZookeeperLauncher(String fileName) throws JsonParseException,
			JsonMappingException, IOException {
		super(fileName);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 2) {
			System.err.println("ZookeeperLauncher <config file> <timeout>");
			System.exit(1);
			return;
		}
		ZookeeperLauncher launcher = new ZookeeperLauncher(argv[0]);
		long timeout = Long.parseLong(argv[1]);
		launcher.start(timeout, TimeUnit.SECONDS);
	}

	// Used in testing
	public final AtomicBoolean configurationCompleted = new AtomicBoolean();
	// Used in testing
	public final AtomicBoolean success = new AtomicBoolean();

	private final Logger LOG = Logger.getLogger(ZookeeperLauncher.class
			.getCanonicalName());
	private QuorumPeer quorumPeer;

	/**
	 * @return the quorumPeer
	 */
	public QuorumPeer getQuorumPeer() {
		return quorumPeer;
	}

	public void fail(Map<String, File> configurations) {
		configurationCompleted.set(true);
		success.set(false);
		System.err.println("Auto configuration of Zookeeper failed");
	}

	public void succeed(Map<String, File> configurations) {
		String configurationFile = configurations.get("zookeeper")
				.getAbsolutePath();
		try {
			initializeAndRun(new String[] { configurationFile });
			configurationCompleted.set(true);
			success.set(true);
		} catch (Throwable e) {
			throw new IllegalStateException(String.format(
					"Unable to start zookeeper using configuration file %s",
					configurationFile), e);
		}
	}

	/**
	 * Copied from QuorumPeerMain because whomever wrote that crap made things
	 * protected. Because freedom.
	 */
	protected void initializeAndRun(String[] args) throws ConfigException,
			IOException {
		QuorumPeerConfig config = new QuorumPeerConfig();
		if (args.length == 1) {
			config.parse(args[0]);
		}

		// Start and schedule the the purge task
		DatadirCleanupManager purgeMgr = new DatadirCleanupManager(
				config.getDataDir(), config.getDataLogDir(),
				config.getSnapRetainCount(), config.getPurgeInterval());
		purgeMgr.start();

		if (args.length == 1 && config.getServers().size() > 0) {
			runFromConfig(config);
		} else {
			LOG.warning("Running in standalone mode");
			// there is only server in the quorum -- run as standalone
			ZooKeeperServerMain.main(args);
		}
	}

	/**
	 * Copied from QuorumPeerMain
	 */
	protected void runFromConfig(QuorumPeerConfig config) throws IOException {
		LOG.info("Starting quorum peer");
		ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
		cnxnFactory.configure(config.getClientPortAddress(),
				config.getMaxClientCnxns());

		quorumPeer = new QuorumPeer();
		quorumPeer.setClientPortAddress(config.getClientPortAddress());
		quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config
				.getDataLogDir()), new File(config.getDataDir())));
		quorumPeer.setQuorumPeers(config.getServers());
		quorumPeer.setElectionType(config.getElectionAlg());
		quorumPeer.setMyid(config.getServerId());
		quorumPeer.setTickTime(config.getTickTime());
		quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
		quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
		quorumPeer.setInitLimit(config.getInitLimit());
		quorumPeer.setSyncLimit(config.getSyncLimit());
		quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
		quorumPeer.setCnxnFactory(cnxnFactory);
		quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
		quorumPeer.setLearnerType(config.getPeerType());

		quorumPeer.start();
	}
}
