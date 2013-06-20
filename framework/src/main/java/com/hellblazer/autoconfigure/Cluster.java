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

import java.util.List;

import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * 
 * @author hal.hildebrand
 * 
 */
public class Cluster<T> {
	private final List<T> cluster;

	public Cluster(List<T> cluster) {
		this.cluster = cluster;
	}

	public int getCardinality() {
		return cluster.size();
	}

	public T getFirst() {
		if (cluster.isEmpty()) {
			throw new STNoSuchPropertyException(null, this, "first");
		}
		return cluster.get(0);
	}

	public List<T> getMembers() {
		return cluster;
	}
	
	@Override
	public String toString() {
		return String.format("Cluster%s", cluster);
	}
}
