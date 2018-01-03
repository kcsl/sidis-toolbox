package com.kcsl.sidis.sid;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ExecutionCounterProbe extends NewProbe {

	private static ExecutionCounterProbe INSTANCE = null;
	
	private ExecutionCounterProbe(){}
	
	public static ExecutionCounterProbe getInstance(){
		if(INSTANCE == null){
			INSTANCE = new ExecutionCounterProbe();
		}
		return INSTANCE;
	}
	
	@Override
	public String getName() {
		return "Execution Counter";
	}

	@Override
	public String getDescription() {
		return "Efficiently counts the number of times a given control flow statement is executed or how many times a control flow transition is taken.";
	}
	
	public static void addProbes(SIDExperiment experiment, Q q){
		for(Node node : new AtlasHashSet<Node>(q.nodes(XCSG.ControlFlow_Node).eval().nodes())){
			addProbe(experiment, node);
		}
		for(Edge edge : new AtlasHashSet<Edge>(q.edges(XCSG.ControlFlow_Edge).eval().edges())){
			addProbe(experiment, edge);
		}
	}
	
	public static void addProbe(SIDExperiment experiment, Node statement){
		validateNode(statement);
		getInstance().addProbeAttributeValue(experiment, statement);
	}
	
	public static void addProbe(SIDExperiment experiment, Edge transition){
		validateEdge(transition);
		getInstance().addProbeAttributeValue(experiment, transition);
	}
	
	public static void removeProbes(SIDExperiment experiment, Q q){
		for(Node node : new AtlasHashSet<Node>(q.nodes(XCSG.ControlFlow_Node).eval().nodes())){
			removeProbe(experiment, node);
		}
		for(Edge edge : new AtlasHashSet<Edge>(q.edges(XCSG.ControlFlow_Edge).eval().edges())){
			removeProbe(experiment, edge);
		}
	}
	
	public static void removeProbe(SIDExperiment experiment, Node statement){
		validateNode(statement);
		getInstance().removeProbeAttributeValue(experiment, statement);
	}
	
	public static void removeProbe(SIDExperiment experiment, Edge transition){
		validateEdge(transition);
		getInstance().removeProbeAttributeValue(experiment, transition);
	}

	private static void validateNode(Node node){
		if(!node.taggedWith(XCSG.ControlFlow_Node)){
			throw new IllegalArgumentException("Node must be an XCSG.ControlFlow_Node");
		}
	}
	
	private static void validateEdge(Edge edge){
		if(!edge.taggedWith(XCSG.ControlFlow_Edge)){
			throw new IllegalArgumentException("Edge must be an XCSG.ControlFlow_Edge");
		}
	}
	
	@Override
	public boolean isSupported(GraphElement graphElement) {
		try {
			if(graphElement instanceof Node){
				validateNode((Node) graphElement);
				return true;
			} else if(graphElement instanceof Node){
				validateEdge((Edge) graphElement);
				return true;
			} else {
				return false;
			}
		} catch (Exception e){
			return false;
		}
	}
	
}
