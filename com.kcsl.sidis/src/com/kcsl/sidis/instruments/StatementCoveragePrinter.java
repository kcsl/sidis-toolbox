package com.kcsl.sidis.instruments;

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
		SootClass printStreamClass = Scene.v().getSootClass("java.io.PrintStream");
		Local sidisPrinter = null;
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasCorrespondence.get(statement);
			if(atlasNode != null && selectedStatements.contains(atlasNode) && !restrictedRegion.contains(atlasNode)){
				if(sidisPrinter == null){
					sidisPrinter = addLocalPrinterReference(methodBody);
				}
				insertPrintBeforeStatement(statements, statement, sidisPrinter, atlasNode.address().toAddressString());
			}
		}
	}
	
	private Local addLocalPrinterReference(Body methodBody){
		Local sidisPrinter = Jimple.v().newLocal("sidisPrinter", RefType.v("java.io.PrintStream"));
		methodBody.getLocals().add(sidisPrinter);
		return sidisPrinter;
	}
	
	private void insertPrintBeforeStatement(Chain<Unit> statements, Unit statement, Local sidisPrinter, String value) {
		// insert "sidisPrinter = com.kcsl.sidis.support.SIDIS.out;"
		statements.insertBefore(
				Jimple.v().newAssignStmt(sidisPrinter,
						Jimple.v().newStaticFieldRef(Scene.v()
								.getField("<com.kcsl.sidis.support.SIDIS: java.io.PrintStream out>").makeRef())),
				statement);
		
		// insert "sidisPrinter.println(<value>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(long)");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sidisPrinter, printlnCallsite.makeRef(), StringConstant.v(value))),
				statement);
	}

}
