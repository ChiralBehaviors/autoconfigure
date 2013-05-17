package com.hellblazer.autoconfigure;

import java.util.List;

public interface Cluster {

	public abstract int getCardinality();

	public abstract Service getFirst();

	public abstract List<Service> getMembers();

}