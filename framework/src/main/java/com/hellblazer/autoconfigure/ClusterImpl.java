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
public class ClusterImpl implements Cluster {
	private final List<Service> cluster;

	public ClusterImpl(List<Service> cluster) {
		this.cluster = cluster;
	}

	/* (non-Javadoc)
	 * @see com.hellblazer.autoconfigure.Cluster#getCardinality()
	 */
	@Override
	public int getCardinality() {
		return cluster.size();
	}

	/* (non-Javadoc)
	 * @see com.hellblazer.autoconfigure.Cluster#getFirst()
	 */
	@Override
	public Service getFirst() {
		if (cluster.isEmpty()) {
			throw new STNoSuchPropertyException(null, this, "first");
		}
		return cluster.get(0);
	}

	/* (non-Javadoc)
	 * @see com.hellblazer.autoconfigure.Cluster#getMembers()
	 */
	@Override
	public List<Service> getMembers() {
		return cluster;
	}
}
