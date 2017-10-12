package com.kcsl.sidis.sid.instruments;

import soot.Transform;

public interface Probe {

	public String getName();
	
	public String getDescription();
	
	public ProbeDataType[] captures();
	
	public Transform getTransform();
	
}
