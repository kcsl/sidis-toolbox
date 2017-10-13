package com.kcsl.sidis.ui.overlay;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.highlight.Highlighter;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.commons.highlighter.HeatMap;
import com.kcsl.sidis.dis.Import;
import com.kcsl.sidis.log.Log;
import com.kcsl.sidis.preferences.SIDISPreferences;

public class HeatMapOverlay {

	public static Highlighter computeHeatMap(AtlasSet<Node> statements){
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
				double intensity = 0.0;
				Long statementExecutionCount = getStatementExecutionCount(statement);
				if(SIDISPreferences.isLogarithmicScaleHeatMapEnabled()){
					intensity = HeatMap.normalizeLogarithmicIntensity(statementExecutionCount, lowestValue, highestValue);
				} else {
					intensity = HeatMap.normalizeIntensity(statementExecutionCount, lowestValue, highestValue);
				}
				
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

	public static Long getStatementExecutionCount(Node statement){
		Object statementCount = statement.getAttr(Import.STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME);
		if(statementCount == null){
			return 0L;
		}
		return Long.parseLong(statementCount.toString());
	}
	
}
