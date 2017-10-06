package com.kcsl.sidis.sid.instruments;

import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.jimple.commons.transform.transforms.MethodCFGTransform;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class StatementCoveragePrinter extends MethodCFGTransform {

	private AtlasSet<Node> selectedStatements;
	
	public StatementCoveragePrinter(Node method, AtlasSet<Node> selectedStatements) {
		super("selected_statement_coverage_printer", method);
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
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void println(java.lang.String)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}

}
