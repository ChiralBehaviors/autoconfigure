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
import java.net.SocketException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.hellblazer.autoconfigure.configuration.Configuration;
import com.hellblazer.autoconfigure.configuration.YamlHelper;
import com.hellblazer.slp.ServiceScope;

/**
 * The abstract class for auto configuring a distributed service.
 * 
 * @author hhildebrand
 * 
 */
public abstract class AutoConfigureService {

    /**
     * @param configurationResource
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public static Configuration configurationFrom(String configurationResource)
                                                                               throws JsonParseException,
                                                                               JsonMappingException,
                                                                               IOException {
        File configFile = new File(configurationResource);
        if (configFile.exists()) {
            return YamlHelper.fromYaml(configFile);
        }
        URL url = getURL(configurationResource);
        if (url == null) {
            throw new IllegalArgumentException(
                                               String.format("No such configuration resource: %s",
                                                             configurationResource));
        }
        return YamlHelper.fromYaml(url.openStream());
    }

    public static URL getURL(String fileName) {
        URL url;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        url = cl.getResource(fileName);
        if (url == null) {
            cl = AutoConfigureService.class.getClassLoader();
            url = cl.getResource(fileName);
        }
        return url;
    }

    private final AutoConfigure autoConfigure;

    public AutoConfigureService(AutoConfigure autoConfigure) {
        this.autoConfigure = autoConfigure;
    }

    /**
     * Construct an instance with the supplied configuration.
     * 
     * @param configuration
     *            - you know what this is.
     * @throws SocketException
     */
    public AutoConfigureService(Configuration configuration)
                                                            throws SocketException {
        this(configuration.construct());
    }

    public AutoConfigureService(String configurationResource)
                                                             throws JsonParseException,
                                                             JsonMappingException,
                                                             IOException {
        this(configurationFrom(configurationResource));
    }

    /**
     * The auto configuration has failed. The map of configurations contains the
     * generated configuration files as configured.
     * 
     * @param configurations
     *            - the map of template names to generated configuration files.
     *            May be incomplete or empty, depending on which phase of the
     *            auto configuration process failed in.
     * @throws Exception
     *             - D'oh!
     */
    abstract public void fail(Map<String, File> configurations)
                                                               throws Exception;

    /**
     * 
     * @return the discovery scope used by this process
     */
    public ServiceScope getDiscoveryScope() {
        return autoConfigure.getDiscoveryScope();
    }

    /**
     * Start the auto configuration service.
     */
    public void start(long timeout, TimeUnit unit) {
        start(null, timeout, unit);
    }

    /**
     * Start the auto configuration service.
     */
    public void start(Map<String, String> environment, long timeout,
                      TimeUnit unit) {
        autoConfigure.configure(environment, this, timeout, unit);
    }

    /**
     * The auto configuration has succeeded. The map of configurations contains
     * the generated configuration files as configured.
     * 
     * @param configurations
     *            - the map of template names to generated configuration files
     * @throws Exception
     *             - D'oh!
     */
    abstract public void succeed(Map<String, File> configurations)
                                                                  throws Exception;
}
