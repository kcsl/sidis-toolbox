package com.kcsl.sidis.dis.ui.codepainter;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CommonQueries;
import com.ensoftcorp.open.commons.codepainter.ColorPalette;
import com.ensoftcorp.open.commons.highlighter.HeatMap;
import com.kcsl.sidis.dis.Import;

public class StatementExecutionHeatMapColorPalette extends ColorPalette {
	
	public static final String NORMALIZE_LOGARITHMIC_SCALE = "Logarithmic Scale";
	public static final String COLD_COLOR = "Cold Color";
	public static final String HOT_COLOR = "Hot Color";
	
	public StatementExecutionHeatMapColorPalette(){
		this.addPossibleFlag("Logarithmic Scale", "Normalizes the heat map intensity to a logarithmic scale", false);
		this.addPossibleParameter(COLD_COLOR, Color.class, "Defines the lowest intensity (cold) value in the heat map color gradient.", Color.BLACK);
		this.addPossibleParameter(HOT_COLOR, Color.class, "Defines the highest intensity (hot) value in the heat map color gradient.", Color.WHITE);
	}
	
	private Map<Node, Color> nodeColors = new HashMap<Node,Color>();
	private Map<Color, String> nodeLegend = new HashMap<Color,String>();
	
	private Map<Edge, Color> edgeColors = new HashMap<Edge,Color>();
	private Map<Color, String> edgeLedgend = new HashMap<Color,String>();
	
	@Override
	public String getName() {
		return "Statement Execution Heat Map";
	}

	@Override
	public String getDescription() {
		return "Displays a heat map style overlay on top of control flow nodes that contain statement execution counts.";
	}

	@Override
	public Map<Node, Color> getNodeColors() {
		return nodeColors;
	}

	@Override
	public Map<Edge, Color> getEdgeColors() {
		return edgeColors;
	}

	@Override
	public Map<Color, String> getNodeColorLegend() {
		return nodeLegend;
	}

	@Override
	public Map<Color, String> getEdgeColorLegend() {
		return edgeLedgend;
	}

	@Override
	protected void canvasChanged() {
		// to make the coloring consistent for any selection we will compute
		// colors for the full function of any statements on the canvas
		Q canvasStatements = Common.toQ(canvas).nodes(XCSG.ControlFlow_Node);
		Q fullCanvas = CommonQueries.cfg(CommonQueries.getContainingFunctions(canvasStatements));
		updateHeatMap(fullCanvas.nodes(XCSG.ControlFlow_Node).eval().nodes());
	}
	
	private void updateHeatMap(AtlasSet<Node> statements){
		nodeColors.clear();
		nodeLegend.clear();
		edgeColors.clear();
		edgeLedgend.clear();
		
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
				double intensity = 0.0;
				Long statementExecutionCount = getStatementExecutionCount(statement);
				if(this.isFlagSet(NORMALIZE_LOGARITHMIC_SCALE)){
					intensity = HeatMap.normalizeLogarithmicIntensity(statementExecutionCount, lowestValue, highestValue);
				} else {
					intensity = HeatMap.normalizeIntensity(statementExecutionCount, lowestValue, highestValue);
				}
				
				Color cold = (Color) this.getParameterValue(COLD_COLOR);
				Color hot = (Color) this.getParameterValue(HOT_COLOR);
				Color color = HeatMap.get2ColorGradientHeatMapColor(intensity, cold, hot);
				nodeColors.put(statement, color);
				nodeLegend.put(color, statementExecutionCount + " (" + intensity + ")");
			}
		}
	}

	private static Long getStatementExecutionCount(Node statement){
		Object statementCount = statement.getAttr(Import.STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME);
		if(statementCount == null){
			return 0L;
		}
		return Long.parseLong(statementCount.toString());
	}

}
