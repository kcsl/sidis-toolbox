package com.kcsl.sidis.sid.instruments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.utilities.address.NormalizedAddress;
import com.ensoftcorp.open.jimple.commons.soot.transforms.MethodCFGTransform;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class BranchCountProbe extends MethodCFGTransform implements Probe {

	@Override
	public String getName() {
		return "Branch Count";
	}

	@Override
	public String getDescription() {
		return "Efficiently counts the number of times a given branch is decision is taken.";
	}

	@Override
	public ProbeDataType[] captures() {
		return new ProbeDataType[]{ ProbeDataType.EXECUTION_COUNTS };
	}
	
	private AtlasSet<Node> selectedBranches;
	
	public BranchCountProbe(Node method, AtlasSet<Node> selectedBranches) {
		super("branch_count_probe", method);
		this.selectedBranches = Common.toQ(cfgNodes).nodes(XCSG.ControlFlowCondition).intersection(Common.toQ(selectedBranches)).eval().nodes();
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence) {
		// get the inverse mapping for later
		Map<Node,Unit> sootStatementCorrespondence = new HashMap<Node,Unit>();
		for(Map.Entry<Unit,Node> entry : atlasControlFlowNodeCorrespondence.entrySet()){
			sootStatementCorrespondence.put(entry.getValue(), entry.getKey());
		}
		
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasNode != null && selectedBranches.contains(atlasNode) && !restrictedRegion.contains(atlasNode)){
				Node branch = atlasNode;
				// if branch is also a successor to another branch selected for instrumentation then we
				// we have a special combined instrument to cut down on the number of insertions
				Q predecessors = Query.universe().edges(XCSG.ControlFlow_Edge).predecessors(Common.toQ(branch));
				if(CommonQueries.isEmpty(predecessors.intersection(Common.toQ(selectedBranches)))){
					String branchAddress = branch.address().toAddressString();
					if(branch.hasAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE)){
						branchAddress = "n_" + branch.getAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE).toString();
					}
					insertPathStartEndBeforeStatement(statements, statement, branchAddress);
				} else {
					String branchAddress = branch.address().toAddressString();
					if(branch.hasAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE)){
						branchAddress = "n_" + branch.getAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE).toString();
					}
					insertPathStartBeforeStatement(statements, statement, branchAddress);
					
					AtlasSet<Node> sucessors = Query.universe().edges(XCSG.ControlFlow_Edge).successors(Common.toQ(branch)).eval().nodes();
					for(Node successor : sucessors){
						if(!restrictedRegion.contains(successor)){
							String statementAddress = successor.address().toAddressString();
							if(successor.hasAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE)){
								statementAddress = "n_" + successor.getAttr(NormalizedAddress.NORMALIZED_ADDRESS_ATTRIBUTE).toString();
							}
							insertPathEndBeforeStatement(statements, sootStatementCorrespondence.get(successor), statementAddress);
						}
					}
				}
			}
		}
	}
	
	private void insertPathStartBeforeStatement(Chain<Unit> statements, Unit statement, String value) {
		// insert "SIDIS.pathStartCount(<address>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void pathStartCount(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}
	
	private void insertPathEndBeforeStatement(Chain<Unit> statements, Unit statement, String value) {
		// insert "SIDIS.pathEndCount(<address>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void pathEndCount(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}
	
	private void insertPathStartEndBeforeStatement(Chain<Unit> statements, Unit statement, String value) {
		// insert "SIDIS.pathStartEndCount(<address>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void pathStartEndCount(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}

}
