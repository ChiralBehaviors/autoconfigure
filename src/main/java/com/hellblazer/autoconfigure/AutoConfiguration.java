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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hellblazer.gossip.configuration.GossipConfiguration;

/**
 * @author hhildebrand
 * 
 */
public class AutoConfiguration {
	@JsonProperty
	public final List<File> configurations = new ArrayList<>();
	@JsonProperty
	public final GossipConfiguration gossip = new GossipConfiguration();
	@JsonProperty
	public final String hostVariable = "myHostName";
	@JsonProperty
	public final int networkInterface = 0;
	@JsonProperty
	public final String portVariable = "myPortNumber";
	@JsonProperty
	public final List<ServiceCollection> serviceCollections = new ArrayList<>();
	@JsonProperty
	public final String serviceUrl = "service:someType:http:%s:%s/myURI";
	@JsonProperty
	public final Map<String, String> serviceProperties = new HashMap<>();
	@JsonProperty
	public final List<Service> services = new ArrayList<>();
	@JsonProperty
	public final Map<String, String> substitutions = new HashMap<>();

}
