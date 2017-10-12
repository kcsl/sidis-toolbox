package com.kcsl.sidis.ui.sid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;
import com.kcsl.sidis.sid.instruments.Probe;
import com.kcsl.sidis.sid.instruments.StatementCountProbe;

public class StatementCounterProbeRequest implements TransformationRequest {

	private HashMap<Node,AtlasSet<Node>> requests = new HashMap<Node,AtlasSet<Node>>();
	
	public StatementCounterProbeRequest(){}
	
	public AtlasSet<Node> getRequestMethods(){
		AtlasSet<Node> methods = new AtlasHashSet<Node>();
		for(Node method : requests.keySet()){
			methods.add(method);
		}
		return methods;
	}
	
	public AtlasSet<Node> getRequestMethodStatements(Node method){
		AtlasSet<Node> statements = new AtlasHashSet<Node>();
		for(Node statement : requests.get(method)){
			statements.add(statement);
		}
		return statements;
	}
	
	public void removeAllStatementProbes(AtlasSet<Node> methods){
		checkMethodInput(methods);
		for(Node method : methods){
			requests.remove(method);
		}
	}
	
	public void addAllStatementProbes(AtlasSet<Node> methods){
		checkMethodInput(methods);
		for(Node method : methods){
			addStatementProbes(method, CommonQueries.cfg(method).eval().nodes());
		}
	}
	
	private void checkMethodInput(AtlasSet<Node> methods) {
		for(Node method : methods){
			if(!method.taggedWith(XCSG.Method)){
				throw new IllegalArgumentException("Node [" + method.address().toAddressString() + "] is not a method.");
			}
		}
		for(Node method : methods){
			if(method.taggedWith(XCSG.abstractMethod)){
				throw new IllegalArgumentException("Method node [" + method.address().toAddressString() + "] is abstract.");
			}
		}
	}
	
	public void addStatementProbes(Node method, AtlasSet<Node> statements){
		checkStatementInputs(method, statements);
		AtlasSet<Node> requestedStatements = requests.remove(method);
		if(requestedStatements == null){
			requestedStatements = new AtlasHashSet<Node>();
		}
		requestedStatements.addAll(statements);
		if(!requestedStatements.isEmpty()){
			requests.put(method, requestedStatements);
		}
	}

	public void removeStatementProbes(Node method, AtlasSet<Node> statements){
		checkStatementInputs(method, statements);
		AtlasSet<Node> requestedStatements = requests.remove(method);
		if(requestedStatements == null){
			requestedStatements = new AtlasHashSet<Node>();
		}
		AtlasSet<Node> statementsToSave = new AtlasHashSet<Node>();
		for(Node requestedStatement : requestedStatements){
			if(!statements.contains(requestedStatement)){
				statementsToSave.add(requestedStatement);
			}
		}
		if(!statementsToSave.isEmpty()){
			requests.put(method, statementsToSave);
		}
	}
	
	private void checkStatementInputs(Node method, AtlasSet<Node> statements) {
		if(!method.taggedWith(XCSG.Method) || method.taggedWith(XCSG.abstractMethod)){
			throw new IllegalArgumentException("Node [" + method.address().toAddressString() + "] is not a concrete method.");
		}
		for(Node statement : statements){
			if(!statement.taggedWith(XCSG.ControlFlow_Node)){
				throw new IllegalArgumentException("Node [" + statement.address().toAddressString() + "] is not a control flow statement.");
			}
		}
		AtlasSet<Node> cfg = CommonQueries.cfg(method).eval().nodes();
		for(Node statement : statements){
			if(!cfg.contains(statement)){
				throw new IllegalArgumentException("Node [" + statement.address().toAddressString() + "] is not a member of the given method [" + method.address().toAddressString() + "].");
			}
		}
	}
	
	public Set<Probe> getProbes(){
		Set<Probe> probes = new HashSet<Probe>();
		for(Entry<Node,AtlasSet<Node>> request : requests.entrySet()){
			Node method = request.getKey();
			AtlasSet<Node> statements = request.getValue();
			StatementCountProbe probe = new StatementCountProbe(method, statements);
			probes.add(probe);
		}
		return probes;
	}
	
	public String getRequestName(String request){
		return getName() + ": " + request;
	}
	
	@Override
	public String getName() {
		return "Statement Counter Probe";
	}

	@Override
	public String getDescription() {
		return "Instruments statements with an in memory counter that increments each time the statement is executed.";
	}

}
