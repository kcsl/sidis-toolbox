package com.kcsl.sidis.sid.instruments;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.jimple.commons.transform.transforms.MethodDFGTransform;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

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
	
	private AtlasSet<Node> selectedStructures;
	private AtlasSet<Node> selectedStatements;
	
	public CardinalityProbe(Node method, AtlasSet<Node> selectedStructures) {
		super("cardinality_probe", method);
		this.selectedStructures = Common.toQ(dfgNodes).intersection(Common.toQ(selectedStructures)).eval().nodes();
		this.selectedStatements = Common.toQ(cfgNodes).intersection(Common.toQ(selectedStructures).parent()).eval().nodes();
	}

	@Override
	protected void transform(Body methodBody, Map<Unit, Node> atlasControlFlowNodeCorrespondence, Map<ValueBox, Node> atlasDataFlowNodeCorrespondence) {
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasControlFlowNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasControlFlowNode != null && selectedStatements.contains(atlasControlFlowNode) && !restrictedRegion.contains(atlasControlFlowNode)){
				List<ValueBox> references = statement.getUseAndDefBoxes();
				for(ValueBox reference : references){
					Node atlasDataFlowNode = atlasDataFlowNodeCorrespondence.get(reference);
					if(atlasDataFlowNode != null && selectedStructures.contains(atlasDataFlowNode)){
						insertPrintAfterStatement(statements, statement, atlasDataFlowNode.address().toAddressString(), reference.getValue());
					}
				}
			}
		}
	}
	
	private void insertPrintAfterStatement(Chain<Unit> statements, Unit statement, String address, Value value) {
		// insert "SIDIS.printCardinality(<address>,<value>);"
		SootMethod printlnCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void printCardinality(java.lang.String,java.lang.Object)");
		statements.insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(printlnCallsite.makeRef(), StringConstant.v(address), value)), statement);
	}

}
