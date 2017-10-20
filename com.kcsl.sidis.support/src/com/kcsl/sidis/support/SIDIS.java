package com.kcsl.sidis.support;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SIDIS {

	private static HashMap<String,Long> counts = new HashMap<String,Long>();
	private static boolean initialized = false;
	private static PrintStream out = getPrintStream();
	
	public static void count(String address){
		if(!initialized){
			initialized = true;
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						FileWriter fw = new FileWriter(new File("sidis.ec.dat"));
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
		Long count = counts.remove(address);
		if(count == null){
			count = 1L;
		} else {
			count++;
		}
		counts.put(address, count);
	}
	
	public static void printCardinality(String address, Object o){
		if(o != null){
			if(o instanceof Collection){
				@SuppressWarnings("rawtypes")
				Collection c = (Collection) o;
				println(address + ":" + c.size());
			} else if(o instanceof Map){
				@SuppressWarnings("rawtypes")
				Map m = (Map) o;
				println(address + ":" + m.size());
			} else if(o instanceof boolean[]){
				boolean[] a = (boolean[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof byte[]){
				byte[] a = (byte[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof short[]){
				short[] a = (short[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof char[]){
				char[] a = (char[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof int[]){
				int[] a = (int[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof long[]){
				long[] a = (long[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof float[]){
				float[] a = (float[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof double[]){
				double[] a = (double[]) o;
				println(address + ":" + a.length);
			} else if(o instanceof Object[]){
				Object[] a = (Object[]) o;
				println(address + ":" + a.length);
			} else {
				// TODO: reflective check if there is a size method that could be called on this
			}
		} else {
			println(address + ":null");
		}
	}

	public static void println(String value){
		out.println(value);
		out.flush();
	}
	
	private static PrintStream getPrintStream(){
		return getPrintStream("sidis.print.dat");
	}
	
	private static PrintStream getPrintStream(String path){
		return getPrintStream(new File(path));
	}
	
	private static PrintStream getPrintStream(File file){
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
