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

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * An adaptor for ST to pull properties from a service
 * 
 * @author hhildebrand
 * 
 */
public class ServiceModelAdaptor implements ModelAdaptor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.stringtemplate.v4.ModelAdaptor#getProperty(org.stringtemplate.v4.
	 * Interpreter, org.stringtemplate.v4.ST, java.lang.Object,
	 * java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getProperty(Interpreter interp, ST self, Object o,
			Object property, String propertyName)
			throws STNoSuchPropertyException {
		Service service = (Service) o;
		if (propertyName.equals("port")) {
			return service.getPort();
		}
		if (propertyName.equals("host")) {
			return service.getHost();
		}
		String prop = service.getProperties().get(propertyName);
		if (prop == null) {
			throw new STNoSuchPropertyException(null, o, propertyName);
		}
		return prop;
	}

}
