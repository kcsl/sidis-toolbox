package com.kcsl.sidis.dis.ui.smart;

import java.awt.Color;
import java.util.HashMap;

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
import com.kcsl.sidis.dis.Import;

/**
 * A Control Flow Smart view that overlays a heat map of statement coverage from dynamic analysis
 * @author Ben Holland
 */
public class ControlFlowHeatMapSmartView extends FilteringAtlasSmartViewScript implements IResizableScript, IExplorableScript {

	@Override
	public String getTitle() {
		return "Control Flow (Dynamic Heat Map)";
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
	
			return computeFrontierResult(origin, cfgs, forward, reverse);
		} else {
			// a function was selected possibly along with cfg nodes
			Q containingFunctions = CommonQueries.getContainingFunctions(origin);
			Q cfgs = CommonQueries.cfg(containingFunctions);
			Q selectedFunctionCFGs = CommonQueries.cfg(Common.toQ(functions));
			
			// just pretend the entire cfg was selected for selected functions
			origin = origin.union(selectedFunctionCFGs);
			return computeFrontierResult(origin, cfgs, forward, reverse);
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
			
			HashMap<Node,Color> colorMap = new HashMap<Node,Color>();
			for(Node statement : statements){
				Long statementExecutionCount = getStatementExecutionCount(statement);
				float percentage = (float) (((double) statementExecutionCount * 100.0) / (double) highestValue);
				colorMap.put(statement, getHeatMapColorValue(percentage));
			}
			
//			// compute a color gradient mapping for each statement
//			Color lowestValueColor = Color.RED.brighter().brighter().brighter();
//			Color highestValueColor = Color.RED.darker().darker().darker();
//			HashMap<Node,Color> colorMap = new HashMap<Node,Color>();
//			for(Node statement : statements){
//				Long statementExecutionCount = getStatementExecutionCount(statement);
//				if(statementExecutionCount == lowestValue){
//					colorMap.put(statement, lowestValueColor);
//				} else if(statementExecutionCount == highestValue){
//					colorMap.put(statement, highestValueColor);
//				} else {
//					long actualRange = highestValue - lowestValue;
//					long colorRange = highestValueColor.getRed() - lowestValueColor.getRed();
//					double interval = (double) actualRange / (double) colorRange;
//					int colorValue = (int) Math.round((double) statementExecutionCount * interval);
//					Color color = new Color(colorValue, 0, 0);
//					colorMap.put(statement, color);
//				}
//			}
		}
		
		return heatMap;
	}
	
	private static Color getHeatMapColorValue(float intensity) {
		float hue = (1f - intensity) * 240f;
		return Color.getHSBColor(hue, 1f, .5f);
	}
	
	private static Long getStatementExecutionCount(Node statement){
		return Long.parseLong(statement.getAttr(Import.STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME).toString());
	}
	
	private FrontierStyledResult computeFrontierResult(Q origin, Q graph, int forward, int reverse){
		// calculate the complete result
		Q fullForward = graph.forward(origin);
		Q fullReverse = graph.reverse(origin);
		Q completeResult = fullForward.union(fullReverse);
		
		// compute what to show for current steps
		Q f = origin.forwardStepOn(completeResult, forward);
		Q r = origin.reverseStepOn(completeResult, reverse);
		Q result = f.union(r).union(origin);
		
		Highlighter heatMap = computeHeatMap(result.nodes(XCSG.ControlFlow_Node).eval().nodes());
		
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
