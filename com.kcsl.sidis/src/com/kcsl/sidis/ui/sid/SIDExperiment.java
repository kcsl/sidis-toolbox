package com.kcsl.sidis.ui.sid;

import java.io.File;

import org.eclipse.core.resources.IProject;

public class SIDExperiment implements Comparable<SIDExperiment> {

	private String name;
	private long createdAt;
	private StatementCounterProbeRequest statementCounterProbeRequest;
	
	private IProject project = null;
	private File jimpleDirectory = null;
	private File originalBytecode = null;
	

	public SIDExperiment(String name){
		this.name = name;
		this.createdAt = System.currentTimeMillis();
		this.statementCounterProbeRequest = new StatementCounterProbeRequest();
	}

	public String getName(){
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	public File getJimpleDirectory() {
		return jimpleDirectory;
	}

	public void setJimpleDirectory(File jimpleDirectory) {
		this.jimpleDirectory = jimpleDirectory;
	}

	public File getOriginalBytecode() {
		return originalBytecode;
	}

	public void setOriginalBytecode(File originalBytecode) {
		this.originalBytecode = originalBytecode;
	}
	
	public StatementCounterProbeRequest getStatementCounterProbeRequest() {
		return statementCounterProbeRequest;
	}
	
	@Override
	public int compareTo(SIDExperiment other) {
		return Long.compare(this.createdAt, other.createdAt);
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
