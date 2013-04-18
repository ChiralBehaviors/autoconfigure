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
import java.util.List;

/**
 * The interface that will be invoked when the autoconfiguration completes,
 * either successfully or unsuccessfully
 * 
 * @author hhildebrand
 * 
 */
public interface ConfiguarationAction {
	/**
	 * Run the system. The supplied list of configuration files represents the
	 * transformed configuration files supplied to the auto configuration
	 * process. These files correspond, in the same order, to the original
	 * configuration files, but have had all the variables substituted.
	 * 
	 * @param transformedConfigurations
	 *            - the list of transformed configuration files
	 */
	void run(List<File> transformedConfigurations);
}
