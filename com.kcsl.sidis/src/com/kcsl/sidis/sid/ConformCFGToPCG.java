package com.kcsl.sidis.sid;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties.Jar;
import com.ensoftcorp.open.jimple.commons.project.ProjectJarJimpleProperties;
import com.ensoftcorp.open.jimple.commons.soot.transforms.MethodCFGTransform;
import com.ensoftcorp.open.pcg.common.PCG;
import com.ensoftcorp.open.pcg.common.PCGFactory;
import com.ensoftcorp.open.slice.analysis.ProgramDependenceGraph;
import com.kcsl.sidis.log.Log;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.util.Chain;

public class ConformCFGToPCG extends MethodCFGTransform {

	public static void testTransform(IProject project, Node method, AtlasSet<Node> eventStatements, File output) throws Exception {
		String task = "Conforming method " +  method.getAttr(XCSG.name) + " in " + project.getName();
		Log.info(task);
		
		for(Jar app : ProjectJarProperties.getApplicationJars(project)) {
			try {
				boolean allowPhantomReferences = ProjectJarJimpleProperties.getJarJimplePhantomReferencesConfiguration(app);
				boolean useOriginalNames = ProjectJarJimpleProperties.getJarJimpleUseOriginalNamesConfiguration(app);
				List<File> libraries = new ArrayList<File>();
				for(Jar lib : ProjectJarProperties.getLibraryJars(project)) {
					libraries.add(lib.getFile());
				}
				boolean outputBytecode = true;
				Transform transform = new ConformCFGToPCG(method, eventStatements).getTransform();
				
				Instrumenter.instrument(app.getFile(), output, libraries, allowPhantomReferences, useOriginalNames, outputBytecode, new Transform[] {transform});
				
				Log.info("Transformation complete (" + output.getAbsolutePath() + ").");
			} catch (Throwable t) {
				Log.error("Fail to perfrom Soot transformation", t);
			}
		}
	}
	
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

		AtlasSet<Edge> irrelevantPaths = pcg.getPCG().difference(pcg.getEvents()).reverseStep(Common.toQ(pcg.getMasterExit())).eval().edges();
		AtlasSet<Node> irrelevantPathSuccessors = new AtlasHashSet<Node>();
		for(Edge irrelevantPath : irrelevantPaths) {
			// ASSERT: from is always an implicit condition since we have removed events
			Node implicitCondition = irrelevantPath.from();
			// get the successor statement of the implicit condition that matches the edge transition (ex: true/false)
			Object conditionValue = irrelevantPath.getAttr(XCSG.conditionValue);
			Node pathSuccessor = Common.toQ(cfg)
					.forwardStep(Common.toQ(implicitCondition))
					.selectEdge(XCSG.conditionValue, conditionValue, conditionValue.toString())
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
		
		// elide execution of irrelevant statements with labels and gotos
		AtlasSet<Node> relevantStatements = pdg.getGraph().reverse(Common.toQ(eventStatements)).eval().nodes();
		AtlasSet<Node> irrelevantStatements = Common.toQ(cfg).retainNodes().difference(Common.toQ(relevantStatements)).eval().nodes();
		
		PCG pcgSlice = PCGFactory.create(Common.toQ(relevantStatements));
		
		AtlasSet<Node> abortedStatements = new AtlasHashSet<Node>();
		Q incomingPCGSliceMasterExistEdges = pcgSlice.getPCG().reverseStep(Common.toQ(pcgSlice.getMasterExit()));
		for(Edge incomingPCGSliceMasterExistEdge : incomingPCGSliceMasterExistEdges.eval().edges()) {
			Node from = incomingPCGSliceMasterExistEdge.from();
			if(incomingPCGSliceMasterExistEdge.hasAttr(XCSG.conditionValue)) {
				Object conditionValue = incomingPCGSliceMasterExistEdge.getAttr(XCSG.conditionValue);
				Q conditionValueEdges = Common.toQ(cfg).selectEdge(XCSG.conditionValue, conditionValue, conditionValue.toString()).retainEdges();
				Q successor = conditionValueEdges.successors(Common.toQ(from));
				abortedStatements.addAll(Common.toQ(cfg).forward(successor).eval().nodes());
			} else {
				abortedStatements.addAll(Common.toQ(cfg).forward(Common.toQ(from)).difference(Common.toQ(from)).eval().nodes());
			}
		}
		
		Q parameterAssignments = CommonQueries.nodesContaining(Common.toQ(cfg), ":= @parameter");
		Q irrelevantBlocks = Common.toQ(irrelevantStatements).induce(Common.toQ(cfg)).difference(Common.toQ(abortedStatements), parameterAssignments);
		AtlasSet<Node> statementsToElide = irrelevantBlocks.eval().nodes();
		
//		// debug
//		DisplayUtils.show(Common.toQ(abortedStatements).induce(Common.toQ(cfg)), "aborted statements");
//		
//		// debug
//		DisplayUtils.show(irrelevantBlocks, "irrelevant blocks");
		
		// perform code transformation
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasNode != null && !restrictedRegion.contains(atlasNode)){
				// mark irrelevance
				if(irrelevantPathSuccessors.contains(atlasNode)) {
					markIrrelevant(statements, statement);
				} else if(endRelevanceNodes.contains(atlasNode)) {
					markEndRelevance(statements, statement);
				}
				
				// remove elided or unreachable code
				if(statementsToElide.contains(atlasNode) || abortedStatements.contains(atlasNode)) {
					statements.remove(statement);
					if(atlasNode.taggedWith(XCSG.controlFlowExitPoint) && !methodBodyUnitsIterator.hasNext()) {
						// we removed the return statement, so we should add a dummy return to bytecode validity
						Q returnsEdges = Common.universe().edgesTaggedWithAny(XCSG.Returns).retainEdges();
						Q voidMethods = returnsEdges.predecessors(Common.types("void"));
						boolean methodIsVoidReturnType = !CommonQueries.isEmpty(Common.toQ(methodNode).intersection(voidMethods));
						if(methodIsVoidReturnType){
							statements.add(Jimple.v().newReturnVoidStmt());
						} else {
							statements.add(Jimple.v().newReturnStmt(NullConstant.v()));
						}
					}
				}
			}
		}
	}
	
	// really this should do some analysis to be safe for inter-procedural calls
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
