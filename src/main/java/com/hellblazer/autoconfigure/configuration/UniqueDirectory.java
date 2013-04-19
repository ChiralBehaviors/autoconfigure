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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.hellblazer.utils.Utils;

/**
 * @author hhildebrand
 * 
 */
public class UniqueDirectory {
	private static final Logger log = Logger.getLogger(UniqueDirectory.class
			.getCanonicalName());

	public File base;
	public String prefix;
	public String suffix;
	public String variable;

	public File resolve() throws IOException {
		File directory = File.createTempFile(prefix, suffix, base)
				.getAbsoluteFile();
		Utils.initializeDirectory(directory);
		log.info(String.format(
				"Created unique directory, label [%s], path [%s]", variable,
				directory.getAbsolutePath()));
		return directory;
	}
}
