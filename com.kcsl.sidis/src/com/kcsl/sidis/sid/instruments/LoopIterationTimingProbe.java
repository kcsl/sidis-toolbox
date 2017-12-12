package com.kcsl.sidis.sid.instruments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.jimple.commons.loops.BoundaryConditions;
import com.ensoftcorp.open.jimple.commons.soot.transforms.MethodCFGTransform;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class LoopIterationTimingProbe extends MethodCFGTransform implements Probe {

	@Override
	public String getName() {
		return "Loop Iteration Timing";
	}

	@Override
	public String getDescription() {
		return "Efficiently logs timestamps between loop iterations.";
	}

	@Override
	public ProbeDataType[] captures() {
		return new ProbeDataType[]{ ProbeDataType.EXECUTION_COUNTS };
	}
	
	private AtlasSet<Node> selectedLoopHeaders;
	
	public LoopIterationTimingProbe(Node method, AtlasSet<Node> selectedStatements) {
		super("loop_iteration_timing_probe", method);
		
//		DisplayUtils.show(Common.toQ(selectedStatements), "selectedStatements");
		
		this.selectedLoopHeaders = Common.toQ(cfgNodes).intersection(Common.toQ(selectedStatements).nodes(XCSG.Loop)).eval().nodes();
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
			if(atlasNode != null && selectedLoopHeaders.contains(atlasNode) && !restrictedRegion.contains(atlasNode)){
				Unit loopHeaderStatement = statement;
				Node loopHeader = atlasNode;
				insertTickBeforeLoopHeaderStatement(statements, loopHeaderStatement, loopHeader.address().toAddressString());

				Q boundaryConditions = Common.toQ(cfgNodes).nodes(BoundaryConditions.BOUNDARY_CONDITION);
				Q loopChildren = Common.universe().edges(XCSG.LoopChild).successors(Common.toQ(loopHeader));
				Q terminators = Common.universe().edges(XCSG.ControlFlow_Edge).successors(boundaryConditions).difference(loopChildren);

				for(Node terminator : terminators.eval().nodes()){
					if(!restrictedRegion.contains(terminator)){
						Unit sootTerminatorStatement = sootStatementCorrespondence.get(terminator);
						if(sootTerminatorStatement != null && !restrictedRegion.contains(terminator)){
							insertTerminateBeforeTerminatorStatement(statements, sootTerminatorStatement, loopHeader.address().toAddressString());
						}
					}
				}
			}
		}
	}
	
	private void insertTickBeforeLoopHeaderStatement(Chain<Unit> statements, Unit statement, String address) {
		// insert "SIDIS.tick(<address>);"
		SootMethod tickCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void tick(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(tickCallsite.makeRef(), StringConstant.v(address))),
				statement);
	}
	
	private void insertTerminateBeforeTerminatorStatement(Chain<Unit> statements, Unit statement, String address) {
		// insert "SIDIS.terminate(<address>);"
		SootMethod terminateCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void terminate(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(terminateCallsite.makeRef(), StringConstant.v(address))),
				statement);
	}

}
