package com.kcsl.sidis.sid.instruments;

import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.jimple.commons.transform.transforms.MethodDFGTransform;

import soot.Body;
import soot.Unit;
import soot.ValueBox;

public class CardinalityProbe extends MethodDFGTransform implements Probe {

	@Override
	public String getName() {
		return "Cardinality";
	}

	@Override
	public String getDescription() {
		return "Efficiently counts the cardinality of a collection, collection-like data structure, or array type.";
	}

	@Override
	public ProbeDataType[] captures() {
		return new ProbeDataType[]{ ProbeDataType.CARDINALITY };
	}
	
	private AtlasSet<Node> selectedStatements;
	
	public CardinalityProbe(Node method, AtlasSet<Node> selectedStatements) {
		super("cardinality_probe", method);
		this.selectedStatements = Common.toQ(cfgNodes).intersection(Common.toQ(selectedStatements)).eval().nodes();
	}

	@Override
	protected void transform(Body methodBody, Map<Unit, Node> atlasControlFlowNodeCorrespondence, Map<ValueBox, Node> atlasDataFlowNodeCorrespondence) {
		// TODO Auto-generated method stub
		
	}
	
	
//	private void insertPrintBeforeStatement(Chain<Unit> statements, Unit statement, String value) {
//		// insert "SIDIS.println(<value>);"
//		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void printCardinality(java.lang.String,java.lang.Object)");
//		
////		statement.
//		
//		statements.insertBefore(Jimple.v().newInvokeStmt(
//				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
//				statement);
//	}

}
