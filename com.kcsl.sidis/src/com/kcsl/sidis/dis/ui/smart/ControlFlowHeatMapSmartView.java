package com.kcsl.sidis.dis.ui.smart;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
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
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.kcsl.sidis.dis.HeatMap;
import com.kcsl.sidis.dis.Import;
import com.kcsl.sidis.log.Log;
import com.kcsl.sidis.preferences.SIDISPreferences;

/**
 * A Control Flow Smart view that overlays a heat map of statement coverage from dynamic analysis
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
		Q origin = filteredSelection.difference(Common.toQ(functions), Common.toQ(dataFlowNodes)).union(Common.toQ(correspondingDataFlowStatements));

		if(functions.isEmpty()){
			// just cfg nodes were selected
			Q containingFunctions = CommonQueries.getContainingFunctions(origin);
			Q cfgs = CommonQueries.cfg(containingFunctions);
	
			Highlighter heatMap = computeHeatMap(cfgs.nodes(XCSG.ControlFlow_Node).eval().nodes());
			return computeFrontierResult(origin, cfgs, forward, reverse, heatMap);
		} else {
			// a function was selected possibly along with cfg nodes
			Q containingFunctions = CommonQueries.getContainingFunctions(origin);
			Q cfgs = CommonQueries.cfg(containingFunctions);
			Q selectedFunctionCFGs = CommonQueries.cfg(Common.toQ(functions));
			
			// just pretend the entire cfg was selected for selected functions
			origin = origin.union(selectedFunctionCFGs);
			
			Highlighter heatMap = computeHeatMap(cfgs.nodes(XCSG.ControlFlow_Node).eval().nodes());
			return computeFrontierResult(origin, cfgs, forward, reverse, heatMap);
		}
	}
	
	private Highlighter computeHeatMap(AtlasSet<Node> statements){
		Highlighter heatMap = new Highlighter();
		
		statements = new AtlasHashSet<Node>(Common.toQ(statements).selectNode(Import.STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME).eval().nodes());
		if(!statements.isEmpty()){
			long lowestValue = Long.MAX_VALUE;
			long highestValue = Long.MIN_VALUE;
			
			// find the min and max statement execution counts
			for(Node statement : statements){
				Long statementExecutionCount = getStatementExecutionCount(statement);
				lowestValue = Math.min(lowestValue, statementExecutionCount);
				highestValue = Math.max(highestValue, statementExecutionCount);
			}
			
			for(Node statement : statements){
				Long statementExecutionCount = getStatementExecutionCount(statement);
				double intensity = HeatMap.normalizeIntensity(statementExecutionCount, lowestValue, highestValue);
				if(SIDISPreferences.isBlueRedColorGradiantEnabled()){
					heatMap.highlightNodes(Common.toQ(statement), HeatMap.getBlueRedGradientHeatMapColor(intensity));
				} else if(SIDISPreferences.isMonochromeColorGradiantEnabled()){
					heatMap.highlightNodes(Common.toQ(statement), HeatMap.getMonochromeHeatMapColor(intensity));
				} else if(SIDISPreferences.isInvertedMonochromeColorGradiantEnabled()){
					heatMap.highlightNodes(Common.toQ(statement), HeatMap.getInvertedMonochromeHeatMapColor(intensity));
				} else {
					Log.warning("No preferred heat map color scheme is configured.");
				}
			}
		}
		
		return heatMap;
	}

	private static Long getStatementExecutionCount(Node statement){
		return Long.parseLong(statement.getAttr(Import.STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME).toString());
	}
	
	private FrontierStyledResult computeFrontierResult(Q origin, Q graph, int forward, int reverse, Highlighter heatMap){
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
		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new MarkupFromH(heatMap));
	}

	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}
	
}
