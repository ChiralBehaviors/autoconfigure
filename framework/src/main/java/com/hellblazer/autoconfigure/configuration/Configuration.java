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
package com.hellblazer.autoconfigure.configuration;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hellblazer.autoconfigure.AutoConfigure;
import com.hellblazer.gossip.configuration.GossipConfiguration;

/**
 * @author hhildebrand
 * 
 */
public class Configuration {
    public static String           RESTART_STATE_FILE = ".autoconfigure.restart.state";

    public List<String>            additionalPorts    = new ArrayList<>();
    public GossipConfiguration     gossip             = new GossipConfiguration();
    public boolean                 ipV6               = false;
    public JmxConfiguration        jmx                = new JmxConfiguration();
    public String                  networkInterface;
    public List<ServiceCollection> serviceCollections = new ArrayList<>();
    public Map<String, String>     serviceProperties  = new HashMap<>();
    public List<SingletonService>  services           = new ArrayList<>();
    public String                  serviceUrl;
    public List<Template>          templates          = new ArrayList<>();
    public String                  totalOrderingFrom;
    public String                  totalOrderingVariable;
    public List<UniqueDirectory>   uniqueDirectories  = new ArrayList<>();
    public Map<String, String>     variables          = new HashMap<>();
    public boolean                 verboseTemplating  = false;
    public String                  restartStateFile   = RESTART_STATE_FILE;

    public Configuration() {

    }

    public Configuration(String serviceUrl, String networkInterface,
                         boolean ipV6, Map<String, String> serviceProperties,
                         List<SingletonService> services,
                         List<ServiceCollection> serviceCollections,
                         List<Template> templates,
                         Map<String, String> variables,
                         List<UniqueDirectory> uniqueDirectories,
                         List<String> additionalPorts,
                         String totalOrderingFrom,
                         String totalOrderingVariable,
                         boolean verboseTemplating, JmxConfiguration jmx,
                         GossipConfiguration gossip, String restartStateFilename) {
        this.serviceUrl = serviceUrl;
        this.networkInterface = networkInterface;
        this.ipV6 = ipV6;
        this.serviceProperties = serviceProperties;
        this.services = services;
        this.serviceCollections = serviceCollections;
        this.templates = templates;
        this.variables = variables;
        this.uniqueDirectories = uniqueDirectories;
        this.additionalPorts = additionalPorts;
        this.totalOrderingFrom = totalOrderingFrom;
        this.totalOrderingVariable = totalOrderingVariable;
        this.verboseTemplating = verboseTemplating;
        this.jmx = jmx;
        if (gossip != null) {
            this.gossip = gossip;
        }
        if (restartStateFilename != null) {
            restartStateFile = restartStateFilename;
        }
    }

    /**
     * Convienence method to construct an autoconfiguration instance.
     * 
     * @return an instance of Autoconfiguration constructed from this
     *         configuration
     * @throws SocketException
     */
    public AutoConfigure construct() throws SocketException {
        return new AutoConfigure(this);
    }
}
