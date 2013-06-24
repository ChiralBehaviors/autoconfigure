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

import java.net.InetSocketAddress;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * Adapt {@link InetSocketAddress} so that it can be used in a similar way to {@link Service} when
 * obtaining host and port. If we didn't register this adaptor then in order get the host portion
 * of the {@link InetSocketAddress} we would have to access it via the "hostname" property/getter
 * which would be inconsistent with how we get the host portion of the configuredService variable
 *
 */
public class InetSocketAddressAdaptor implements ModelAdaptor {

    @Override
    public Object getProperty(Interpreter interp, ST self, Object o,
                              Object property, String propertyName)
            throws STNoSuchPropertyException {
        InetSocketAddress address = (InetSocketAddress) o;
        if (propertyName.equals("port")) {
            return address.getPort();
        }
        // adapt hostName -> host
        if (propertyName.equals("host")) {
            return address.getHostName();
        }
        throw new STNoSuchPropertyException(null, o, propertyName);
    }

}
