package com.kcsl.sidis.sid;

import java.io.File;

import soot.Transform;

@SuppressWarnings("unused")
public class SIDExperiment implements Comparable<SIDExperiment> {

	private String name;
	private long createdAt;
	
	private static String[] PROBE_TYPES = new String[]{};

	public SIDExperiment(String name){
		this.name = name;
		this.createdAt = System.currentTimeMillis();
	}

	public String getName(){
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	private Transform[] generateTransforms(){
		return null;
	}
	
	public void generate(String output){
		generate(new File(output));
	}
	
	public void generate(File output){
		// TODO: implement
	}
	
	@Override
	public int compareTo(SIDExperiment other) {
		int nameComparison = this.name.compareTo(other.name);
		if(nameComparison == 0){
			return Long.compare(this.createdAt, other.createdAt);
		} else {
			return nameComparison;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (createdAt ^ (createdAt >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SIDExperiment other = (SIDExperiment) obj;
		if (createdAt != other.createdAt)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
