package com.kcsl.sidis.dis.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CallSiteAnalysis;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.codepainter.CodePainter;
import com.ensoftcorp.open.commons.codepainter.ColorPalette;

/**
 * A Control Flow code painter that overlays a heat map of statement coverage from
 * dynamic analysis
 * 
 * @author Ben Holland
 */
public class ControlFlowHeatMapCodePainter extends CodePainter {

	private ColorPalette heatMapColorPalette = new HeatMapColorPalette();
	
	@Override
	public String getTitle() {
		return "Control Flow (DIS Heat Map)";
	}
	
	@Override
	public String getCategory() {
		return "SIDIS";
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.DataFlow_Node, XCSG.ControlFlow_Node, XCSG.Function };
	}
	
	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}
	
	public Q convertSelection(Q filteredSelections){
		Q dataFlowNodes = filteredSelections.nodes(XCSG.DataFlow_Node);
		Q controlFlowNodes = filteredSelections.nodes(XCSG.ControlFlow_Node);
		Q functions = filteredSelections.nodes(XCSG.Function);
		
		// convert data flow nodes to control flow nodes
		return controlFlowNodes.union(functions, dataFlowNodes.parent());
	}

	@Override
	public int getDefaultStepTop() {
		return 1;
	}

	@Override
	public int getDefaultStepBottom() {
		return 1;
	}

	@Override
	public ColorPalette getBaseColorPalette() {
		return heatMapColorPalette;
	}

	@Override
	public UnstyledFrontierResult computeFrontierResult(Q filteredSelections, int reverse, int forward) {
		if(filteredSelections.eval().nodes().isEmpty()){
			return null;
		}
		
		AtlasSet<Node> dataFlowNodes = filteredSelections.nodes(XCSG.DataFlow_Node).eval().nodes();
		AtlasSet<Node> correspondingDataFlowStatements = Common.toQ(dataFlowNodes).parent().nodes(XCSG.ControlFlow_Node).eval().nodes();
		AtlasSet<Node> functions = filteredSelections.nodes(XCSG.Function).eval().nodes();
		Q selectedStatements = filteredSelections.difference(Common.toQ(functions), Common.toQ(dataFlowNodes)).union(Common.toQ(correspondingDataFlowStatements));
		Q containingFunctions = CommonQueries.getContainingFunctions(selectedStatements);
		
		if(functions.isEmpty()){
			// just cfg nodes were selected
			Q cfgs = getCFG(containingFunctions);

			heatMapColorPalette.setCanvas(cfgs.nodes(XCSG.ControlFlow_Node));
			
			UnstyledFrontierResult frontier = computeFrontierResult(selectedStatements, cfgs, reverse, forward);
			
			// a selection could include a function, so explicitly include it in the result to be highlighted
			Q result = frontier.getResult().union(filteredSelections.nodes(XCSG.Function));
			return new UnstyledFrontierResult(result, frontier.getFrontierReverse(), frontier.getFrontierForward());
		} else {
			// a function was selected possibly along with cfg nodes
			Q cfgs = getCFG(containingFunctions);
			Q selectedFunctions = Common.toQ(functions);
			
			// remove any functions that are selected because callsites were selected
			Q selectedCallsites = selectedStatements.children().nodes(XCSG.CallSite);
			Q selectedCallsiteFunctions = CallSiteAnalysis.getTargets(selectedCallsites);
			selectedFunctions = selectedFunctions.difference(selectedCallsiteFunctions);
			
			// get the complete CFGs for any intentionally selected function
			Q selectedFunctionCFGs = getCFG(selectedFunctions);
			
			// just pretend the entire cfg was selected for selected functions
			selectedStatements = selectedStatements.union(selectedFunctionCFGs);
			
			Q allCFGs = cfgs.union(selectedFunctionCFGs);
			
			heatMapColorPalette.setCanvas(allCFGs.nodes(XCSG.ControlFlow_Node));
			
			UnstyledFrontierResult frontier = computeFrontierResult(selectedStatements, allCFGs, reverse, forward);
			
			// a selection could include a function, so explicitly include it in the result to be highlighted
			Q result = frontier.getResult().union(filteredSelections.nodes(XCSG.Function));
			return new UnstyledFrontierResult(result, frontier.getFrontierReverse(), frontier.getFrontierForward());
		}
	}
	
	protected Q getCFG(Q functions){
		return CommonQueries.cfg(functions);
	}
	
	private UnstyledFrontierResult computeFrontierResult(Q origin, Q graph, int reverse, int forward){
		// compute what to show for current steps
		Q f = origin.forwardStepOn(graph, forward);
		Q r = origin.reverseStepOn(graph, reverse);
		Q result = f.union(r);
		
		// compute what is on the frontier
		Q frontierReverse = origin.reverseStepOn(graph, reverse+1);
		frontierReverse = frontierReverse.differenceEdges(result).retainEdges();
		Q frontierForward = origin.forwardStepOn(graph, forward+1);
		frontierForward = frontierForward.differenceEdges(result).retainEdges();
		
		// show the result
		return new UnstyledFrontierResult(result, frontierReverse, frontierForward);
	}
	
}
