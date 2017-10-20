package com.kcsl.sidis.sid.instruments;

import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.jimple.commons.transform.transforms.MethodCFGTransform;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class StatementExecutionProbe extends MethodCFGTransform implements Probe {

	@Override
	public String getName() {
		return "Statement Execution";
	}

	@Override
	public String getDescription() {
		return "Records when a statement is executed relative to other statements.";
	}

	@Override
	public ProbeDataType[] captures() {
		return new ProbeDataType[]{ ProbeDataType.EXECUTION_COUNTS, ProbeDataType.EXECUTION_ORDERINGS };
	}
	
	private AtlasSet<Node> selectedStatements;
	
	public StatementExecutionProbe(Node method, AtlasSet<Node> selectedStatements) {
		super("statement_execution_probe", method);
		this.selectedStatements = Common.toQ(cfgNodes).intersection(Common.toQ(selectedStatements)).eval().nodes();
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence) {
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasNode != null && selectedStatements.contains(atlasNode) && !restrictedRegion.contains(atlasNode)){
				insertPrintBeforeStatement(statements, statement, atlasNode.address().toAddressString());
			}
		}
	}
	
	private void insertPrintBeforeStatement(Chain<Unit> statements, Unit statement, String address) {
		// insert "SIDIS.println(<address>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void println(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(address))),
				statement);
	}

}
