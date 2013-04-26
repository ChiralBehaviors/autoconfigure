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
package com.hellblazer.autoconfigure.wka;

import java.io.File;

import com.hellblazer.gossip.Gossip;
import com.hellblazer.gossip.configuration.GossipConfiguration;
import com.hellblazer.gossip.configuration.YamlHelper;

/**
 * A simple process that serves as the well known address, providing a gossip
 * seed for the discovery fabric.
 * 
 * @author hhildebrand
 * 
 */
public class WellKnownAddress {
	private final Gossip gossip;

	public static void main(String[] argv) throws Exception {
		if (argv.length == 0) {
			System.err.println("Usage: WellKnownAddress <config file name>");
			System.exit(1);
		}
		GossipConfiguration config = YamlHelper.fromYaml(new File(argv[0]));
		WellKnownAddress wka = new WellKnownAddress(config.construct());
		wka.start();
	}

	public WellKnownAddress(Gossip gossip) {
		this.gossip = gossip;
	}

	public void start() {
		gossip.start();
	}
}
