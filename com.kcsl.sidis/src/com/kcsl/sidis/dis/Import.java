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
import com.kcsl.sidis.log.Log;

public class Import {
	
	public static final String STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME = "STATEMENT_EXECUTION_COUNT_ATTRIBUTE"; 
	
	public static void loadStatementCoverageData(File file, boolean clearPreviousData) throws FileNotFoundException {
		
		purgeStatementExecutionCounts(clearPreviousData);
		
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
		scanner.close();
		
		saveStatementExecutionCounts(statementCounts);
	}
	
	public static void loadStatementExecutionCountData(File file, boolean clearPreviousData) throws FileNotFoundException {
		
		// purge old data
		purgeStatementExecutionCounts(clearPreviousData);
		
		// count how many times each statement was executed
		Map<String,Long> statementCounts = new HashMap<String,Long>();
		Scanner scanner = new Scanner(file);
		while(scanner.hasNextLine()){
			String line = scanner.nextLine().trim();
			if(!line.isEmpty()){
				String[] parts = line.split(":");
				if(parts.length == 2){
					String address = parts[0];
					Long value = Long.parseLong(parts[1]);
					Long count = statementCounts.remove(address);
					if(count == null){
						count = 0L;
					}
					count += value;
					statementCounts.put(address, count);
				}
			}
		}
		scanner.close();
		
		// attribute each statement
		saveStatementExecutionCounts(statementCounts);
	}

	private static void saveStatementExecutionCounts(Map<String, Long> statementCounts) {
		for(Entry<String,Long> entry : statementCounts.entrySet()){
			String address = entry.getKey();
			String count = entry.getValue().toString();
			Node statement = CommonQueries.getNodeByAddress(address);
			if(statement != null){
				statement.putAttr(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME, count);
			} else {
				Log.warning("Statement: " + address + " does not exist.");
			}
		}
	}

	private static void purgeStatementExecutionCounts(boolean clearPreviousData) {
		if(clearPreviousData){
			AtlasSet<Node> attributedStatements = new AtlasHashSet<Node>(Common.universe().selectNode(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME).eval().nodes());
			for(Node attributedStatement : attributedStatements){
				attributedStatement.removeAttr(STATEMENT_EXECUTION_COUNT_ATTRIBUTE_NAME);
			}
		}
	}
	
}
