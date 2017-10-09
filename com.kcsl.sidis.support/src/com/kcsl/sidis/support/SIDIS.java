package com.kcsl.sidis.support;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

public class SIDIS {

	private static HashMap<String,Long> counts = new HashMap<String,Long>();
	private static boolean initialized = false;
	
	public static void count(String value){
		if(!initialized){
			initialized = true;
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						FileWriter fw = new FileWriter(new File("sidis.dat"));
						for(Entry<String,Long> entry : counts.entrySet()){
							fw.write(entry.getKey() + ":" + entry.getValue() + "\n");
						}
						fw.flush();
						fw.close();
					} catch (IOException e){
						System.err.println(e);
					}
				}
			}));
		}
		Long count = counts.remove(value);
		if(count == null){
			count = 1L;
		} else {
			count++;
		}
		counts.put(value, count);
	}
	
	public static PrintStream out = getPrintStream();
	
	public static void println(String value){
		out.println(value);
		out.flush();
	}
	
	public static PrintStream getPrintStream(){
		return getPrintStream("sidis.dat");
	}
	
	public static PrintStream getPrintStream(String path){
		return getPrintStream(new File(path));
	}
	
	public static PrintStream getPrintStream(File file){
		PrintStream result = null;
		try {
			result = new PrintStream(file);
		} catch (IOException e){
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}
	
}
