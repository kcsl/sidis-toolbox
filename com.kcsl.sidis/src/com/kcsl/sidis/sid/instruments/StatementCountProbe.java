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

public class StatementCountProbe extends MethodCFGTransform implements Probe {

	@Override
	public String getName() {
		return "Statement Count";
	}

	@Override
	public String getDescription() {
		return "Efficiently counts the number of times a given statement is executed.";
	}

	@Override
	public ProbeDataType[] captures() {
		return new ProbeDataType[]{ ProbeDataType.EXECUTION_COUNTS };
	}
	
	private AtlasSet<Node> selectedStatements;
	
	public StatementCountProbe(Node method, AtlasSet<Node> selectedStatements) {
		super("statement_count_probe", method);
		this.selectedStatements = Common.toQ(cfgNodes).intersection(Common.toQ(selectedStatements)).eval().nodes();
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasCorrespondence) {
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasCorrespondence.get(statement);
			if(atlasNode != null && selectedStatements.contains(atlasNode) && !restrictedRegion.contains(atlasNode)){
				insertPrintBeforeStatement(statements, statement, atlasNode.address().toAddressString());
			}
		}
	}
	
	private void insertPrintBeforeStatement(Chain<Unit> statements, Unit statement, String value) {
		// insert "SIDIS.println(<value>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void count(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}

}
