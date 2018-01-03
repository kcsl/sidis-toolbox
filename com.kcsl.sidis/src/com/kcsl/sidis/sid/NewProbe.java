package com.kcsl.sidis.sid;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;

public abstract class NewProbe {
	
	public static final String EXPERIMENT_PROBES_ATTRIBUTE_PREFIX = "SIDExperimentProbes";

	public abstract String getName();
	
	public abstract String getDescription();
	
	public abstract boolean isSupported(GraphElement graphElement);
	
	public String getIdentifier(){
		return this.getClass().getSimpleName();
	}
	
	private String getAttributeName(SIDExperiment experiment){
		return EXPERIMENT_PROBES_ATTRIBUTE_PREFIX + "_" + experiment.getName();
	}
	
	protected boolean addProbeAttributeValue(SIDExperiment experiment, GraphElement graphElement){
		return addProbeAttributeValue(experiment, graphElement, null);
	}
	
	private boolean addProbeAttributeValue(SIDExperiment experiment, GraphElement graphElement, String content){
		Object value = graphElement.getAttr(getAttributeName(experiment));
		if(graphElement.hasAttr(getAttributeName(experiment)) && value != null && value instanceof String){
			String valueString = (String) value;
			for(String probe : valueString.split(",")){
				String probeName = probe;
				if(probeName.contains(":")){
					probeName = probeName.split(":")[0];
				}
				if(probeName.equals(getIdentifier())){
					return false;
				}
			}
			graphElement.putAttr(getAttributeName(experiment), valueString + "," + (getIdentifier() + content != null ? (":" + content) : ""));
			return true;
		} else {
			graphElement.putAttr(getAttributeName(experiment), getIdentifier());
			return true;
		}
	}
	
	protected void setProbeAttributeValueContent(SIDExperiment experiment, GraphElement graphElement, String content){
		removeProbeAttributeValue(experiment, graphElement);
		addProbeAttributeValue(experiment, graphElement, content);
	}
	
	protected boolean removeProbeAttributeValue(SIDExperiment experiment, GraphElement graphElement){
		boolean resultChanged = false;
		Object value = graphElement.getAttr(getAttributeName(experiment));
		if(graphElement.hasAttr(getAttributeName(experiment)) && value != null && value instanceof String){
			String valueString = (String) value;
			StringBuilder result = new StringBuilder();
			String prefix = "";
			for(String probe : valueString.split(",")){
				String probeName = probe;
				if(probeName.contains(":")){
					probeName = probeName.split(":")[0];
				}
				if(!probeName.equals(getIdentifier())){
					result.append(prefix + probe);
					if(prefix.isEmpty()){
						prefix = ",";
					}
					resultChanged = true;
				}
			}
			graphElement.putAttr(getAttributeName(experiment), result.toString());
		}
		return resultChanged;
	}
}
