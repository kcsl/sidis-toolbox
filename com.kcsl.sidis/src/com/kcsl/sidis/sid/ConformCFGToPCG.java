package com.kcsl.sidis.sid;

import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.utilities.address.NormalizedAddress;
import com.ensoftcorp.open.jimple.commons.soot.transforms.MethodCFGTransform;
import com.ensoftcorp.open.pcg.common.PCG;
import com.ensoftcorp.open.pcg.common.PCGFactory;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class ConformCFGToPCG extends MethodCFGTransform {

	private AtlasSet<Node> eventStatements;
	private Graph cfg;
	private Graph dfg;
	private PCG pcg;
	private ProgramDependenceGraph pdg;
	
	public ConformCFGToPCG(Node method, AtlasSet<Node> eventStatements) {
		super("conform_cfg_to_pcg", method);
		this.eventStatements = Common.toQ(cfgNodes).intersection(Common.toQ(eventStatements)).eval().nodes();
		cfg = CommonQueries.cfg(method).eval();
		dfg = CommonQueries.dfg(method).eval();
		pdg = new ProgramDependenceGraph(cfg, dfg);
		pcg = PCGFactory.create(Common.toQ(eventStatements));
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence) {
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		
		AtlasSet<Node> sliceStatements = pdg.getGraph().reverse(Common.toQ(eventStatements)).eval().nodes();
		
		AtlasSet<Edge> irrelevantPaths = pcg.getPCG().difference(pcg.getEvents()).reverseStep(Common.toQ(pcg.getMasterExit())).eval().edges();
		AtlasSet<Node> irrelevantPathSuccessors = new AtlasHashSet<Node>();
		for(Edge irrelevantPath : irrelevantPaths) {
			// ASSERT: from is always an implicit condition since we have removed events
			Node implicitCondition = irrelevantPath.from();
			// get the successor statement of the implicit condition that matches the edge transition (ex: true/false)
			Node pathSuccessor = Common.toQ(cfg)
					.forwardStep(Common.toQ(implicitCondition))
					.selectEdge(XCSG.conditionValue, irrelevantPath.getAttr(XCSG.conditionValue))
					.eval().edges().one().to();
			irrelevantPathSuccessors.add(pathSuccessor);
		}
		
		AtlasSet<Node> endRelevanceNodes = new AtlasHashSet<Node>();
		for(Edge endRelevanceEdge : pcg.getPCG().betweenStep(pcg.getEvents(), Common.toQ(pcg.getMasterExit())).eval().edges()) {
			Node event = endRelevanceEdge.from();
			// get successors that are not self-successors (loops), which are the last events in the pcg before the master exit
			for(Node eventSuccessor : Common.toQ(cfg).successors(Common.toQ(event)).difference(Common.toQ(event)).eval().nodes()) {
				endRelevanceNodes.add(eventSuccessor);
			}
		}
		
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasNode != null && !restrictedRegion.contains(atlasNode)){
				if(irrelevantPathSuccessors.contains(atlasNode)) {
					markIrrelevant(statements, statement);
				} else if(endRelevanceNodes.contains(atlasNode)) {
					markEndRelevance(statements, statement);
				}
			}
		}
	}
	
	// really this should do some analysis to be safe for inter-procedural calls
	// right now it just shuts down the vm
	// for inter-procedural calls we could jump we could just return if the slice of the reachable return does not include
	// statements after the events and there are no other inter-procedural side effects
	private void markEndRelevance(Chain<Unit> statements, Unit statement) {
		// insert "SIDIS.endRelevance();"
		SootMethod crashCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void endRelevance()");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(crashCallsite.makeRef())),
				statement);
	}
	
	private void markIrrelevant(Chain<Unit> statements, Unit statement) {
		// insert "SIDIS.irrelevant();"
		SootMethod crashCallsite = Scene.v().getSootClass("com.kcsl.sidis.support.SIDIS").getMethod("void irrelevant()");
		statements.insertBefore(Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(crashCallsite.makeRef())),
				statement);
	}

}
