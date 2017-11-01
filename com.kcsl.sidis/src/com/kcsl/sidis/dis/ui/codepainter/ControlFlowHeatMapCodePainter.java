package com.kcsl.sidis.dis.ui.codepainter;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
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
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.DataFlow_Node, XCSG.ControlFlow_Node, XCSG.Function };
	}
	
	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
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
	public UnstyledFrontierResult computeFrontierResult(IAtlasSelectionEvent event, int reverse, int forward) {
		Q filteredSelection = filter(event.getSelection());

		if(filteredSelection.eval().nodes().isEmpty()){
			return null;
		}
		
		AtlasSet<Node> dataFlowNodes = filteredSelection.nodes(XCSG.DataFlow_Node).eval().nodes();
		AtlasSet<Node> correspondingDataFlowStatements = Common.toQ(dataFlowNodes).parent().nodes(XCSG.ControlFlow_Node).eval().nodes();
		AtlasSet<Node> functions = filteredSelection.nodes(XCSG.Function).eval().nodes();
		Q selectedStatements = filteredSelection.difference(Common.toQ(functions), Common.toQ(dataFlowNodes)).union(Common.toQ(correspondingDataFlowStatements));

		if(functions.isEmpty()){
			// just cfg nodes were selected
			Q containingFunctions = CommonQueries.getContainingFunctions(selectedStatements);
			Q cfgs = CommonQueries.cfg(containingFunctions);

			heatMapColorPalette.setCanvas(cfgs.nodes(XCSG.ControlFlow_Node));
			
			return computeFrontierResult(selectedStatements, cfgs, forward, reverse);
		} else {
			// a function was selected possibly along with cfg nodes
			Q containingFunctions = CommonQueries.getContainingFunctions(selectedStatements);
			Q cfgs = CommonQueries.cfg(containingFunctions);
			Q selectedFunctions = Common.toQ(functions);
			
			// remove any functions that are selected because callsites were selected
			Q selectedCallsites = selectedStatements.children().nodes(XCSG.CallSite);
			Q selectedCallsiteFunctions = CallSiteAnalysis.getTargets(selectedCallsites);
			selectedFunctions = selectedFunctions.difference(selectedCallsiteFunctions);
			
			// get the complete CFGs for any intentionally selected function
			Q selectedFunctionCFGs = CommonQueries.cfg(selectedFunctions);
			
			// just pretend the entire cfg was selected for selected functions
			selectedStatements = selectedStatements.union(selectedFunctionCFGs);
			
			heatMapColorPalette.setCanvas(cfgs.union(selectedFunctionCFGs).nodes(XCSG.ControlFlow_Node));
			
			return computeFrontierResult(selectedStatements, cfgs, forward, reverse);
		}
	}
	
	private UnstyledFrontierResult computeFrontierResult(Q origin, Q graph, int forward, int reverse){
		// calculate the complete result
		Q fullForward = graph.forward(origin);
		Q fullReverse = graph.reverse(origin);
		Q completeResult = fullForward.union(fullReverse);
		
		// compute what to show for current steps
		Q f = origin.forwardStepOn(completeResult, forward);
		Q r = origin.reverseStepOn(completeResult, reverse);
		Q result = f.union(r).union(origin);
		
		// compute what is on the frontier
		Q frontierForward = origin.forwardStepOn(completeResult, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		Q frontierReverse = origin.reverseStepOn(completeResult, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		// show the result
		return new UnstyledFrontierResult(result, frontierReverse, frontierForward);
	}
	
}
