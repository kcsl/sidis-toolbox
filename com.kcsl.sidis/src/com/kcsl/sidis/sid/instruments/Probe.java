package com.kcsl.sidis.sid.instruments;

public interface Probe {

	public String getName();
	
	public String getDescription();
	
	public ProbeDataType[] captures();
	
}
