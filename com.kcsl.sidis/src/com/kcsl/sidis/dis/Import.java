package com.kcsl.sidis.dis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.commons.analysis.CommonQueries;

public class Import {
	
	public static final String STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME = "STATEMENT_EXECUTION_COUNT_ATTRIBUTE"; 
	
	public static void loadStatementCoverageData(File file, boolean clearPreviousData) throws FileNotFoundException {
		
		// purge old data
		if(clearPreviousData){
			AtlasSet<Node> attributedStatements = new AtlasHashSet<Node>(Common.universe().selectNode(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME).eval().nodes());
			for(Node attributedStatement : attributedStatements){
				attributedStatement.removeAttr(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME);
			}
		}
		
		// count how many times each statement was executed
		Map<String,Long> statementCounts = new HashMap<String,Long>();
		Scanner scanner = new Scanner(file);
		while(scanner.hasNextLine()){
			String address = scanner.nextLine();
			Long count = statementCounts.remove(address);
			if(count == null){
				count = 1L;
			} else {
				count++;
			}
			statementCounts.put(address, count);
		}
		
		// attribute each statement
		for(Entry<String,Long> entry : statementCounts.entrySet()){
			String address = entry.getKey();
			String count = entry.getValue().toString();
			Node statement = CommonQueries.getNodeByAddress(address);
			statement.putAttr(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME, count);
		}
	}
	
}
