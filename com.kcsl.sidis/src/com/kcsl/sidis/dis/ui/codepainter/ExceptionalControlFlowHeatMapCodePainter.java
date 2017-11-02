package com.kcsl.sidis.dis.ui.codepainter;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.open.commons.analysis.CommonQueries;

public class ExceptionalControlFlowHeatMapCodePainter extends ControlFlowHeatMapCodePainter {

	@Override
	public String getTitle(){
		return "Exceptional " + super.getTitle();
	}
	
	@Override
	protected Q getCFG(Q functions){
		return CommonQueries.excfg(functions);
	}
	
}
