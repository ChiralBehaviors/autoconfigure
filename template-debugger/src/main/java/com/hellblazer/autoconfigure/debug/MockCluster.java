package com.hellblazer.autoconfigure.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import com.hellblazer.autoconfigure.Service;

public class MockCluster {
	private final List<Map<String, String>> cluster = new ArrayList<>();

	public MockCluster(List<Map<String, String>> members) {
		this.cluster.addAll(members);
	}

	public int getCardinality() {
		return cluster.size();
	}

	public Service getFirst() {
		throw new STNoSuchPropertyException(null, this, "first");
	}

	public List<Map<String, String>> getMembers() {
		return cluster;
	}
}
