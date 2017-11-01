package com.kcsl.sidis.dis.ui.smart;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.markup.MarkupFromH;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IExplorableScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.scripts.util.SimpleScriptUtil;
import com.ensoftcorp.atlas.ui.selection.event.FrontierEdgeExploreEvent;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.commons.analysis.CallSiteAnalysis;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.kcsl.sidis.ui.overlay.HeatMapOverlay;

/**
 * A Control Flow Smart view that overlays a heat map of statement coverage from
 * dynamic analysis
 * 
 * @author Ben Holland
 */
public class ControlFlowHeatMapSmartView extends FilteringAtlasSmartViewScript implements IResizableScript, IExplorableScript {

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
	public FrontierStyledResult explore(FrontierEdgeExploreEvent event, FrontierStyledResult oldResult) {
		return SimpleScriptUtil.explore(this, event, oldResult);
	}

	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
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
	
			Highlighter heatMap = HeatMapOverlay.computeHeatMap(cfgs.nodes(XCSG.ControlFlow_Node).eval().nodes());
			return computeFrontierResult(selectedStatements, cfgs, forward, reverse, heatMap);
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
			
			Highlighter heatMap = HeatMapOverlay.computeHeatMap(cfgs.union(selectedFunctionCFGs).nodes(XCSG.ControlFlow_Node).eval().nodes());
			return computeFrontierResult(selectedStatements, cfgs, forward, reverse, heatMap);
		}
	}
	
	private FrontierStyledResult computeFrontierResult(Q origin, Q graph, int forward, int reverse, Highlighter heatMap){
		// compute what to show for current steps
		Q f = origin.forwardStepOn(graph, forward);
		Q r = origin.reverseStepOn(graph, reverse);
		Q result = f.union(r);
		
		// compute what is on the frontier
		Q frontierForward = origin.forwardStepOn(graph, forward+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		Q frontierReverse = origin.reverseStepOn(graph, reverse+1);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		// show the result
		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(heatMap));
	}

	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}
	
}
