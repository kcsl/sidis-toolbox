package com.kcsl.sidis.sid.instruments;

import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.jimple.commons.transform.transforms.MethodCFGTransform;
import com.kcsl.sidis.log.Log;

import soot.Body;
import soot.Unit;
import soot.util.Chain;

public class LoopHeaderCounter extends MethodCFGTransform {

	private AtlasSet<Node> loopHeaders;
	
	public LoopHeaderCounter(Node method, AtlasSet<Node> loopHeaders) {
		super("loop_header_counter", method);
		this.loopHeaders = Common.toQ(cfgNodes).intersection(Common.toQ(loopHeaders)).eval().nodes();
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasCorrespondence) {
		Chain<Unit> methodBodyUnits = methodBody.getUnits();
		for(Unit unit : methodBodyUnits){
			if(loopHeaders.contains(atlasCorrespondence.get(unit))){
				Log.info(unit.toString());
			}
		}
	}

}
