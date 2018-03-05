package com.kcsl.sidis.support;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SIDIS {
	
	private static boolean initialized = false;
	private static PrintStream out = null;
	
	private static String predecessor = null;
	private static Map<String,Long> pathCounts = Collections.synchronizedMap(new HashMap<String,Long>());
	
	private static boolean abortHooks = false;
	
	public static synchronized void irrelevant(){
		
		throw new Error("sidis-irrelevant");
		
//		// TODO: signal to Daikon this execution should not be analyzed
//		
//		// don't save any probe information
//		abortHooks = true;
//		
//		System.exit(0); // exit gracefully, we don't want to count this as a crash (just as not interesting)
	}
	
	public static synchronized void endRelevance(){
		
		throw new Error("sidis-end-relevance");
		
//		// save the probes this was on a relevant path
//		System.exit(0); // exit gracefully, we don't want to count this as a crash (just no longer interesting)
	}
	
	public static synchronized void pathStartEndCount(String address){
		pathEndCount(address); // this is the successor to a previous predecessor
		pathStartCount(address); // this is also the predecessor to another successor
	}
	
	public static synchronized void pathStartCount(String address){
		if(!initialized){
			initialized = true;
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						if(!abortHooks) {
							FileWriter fw = new FileWriter(new File("sidis.pc.dat"));
							for(Entry<String,Long> entry : pathCounts.entrySet()){
								fw.write(entry.getKey() + ":" + entry.getValue() + "\n");
							}
							fw.flush();
							fw.close();
						}
					} catch (IOException e){
						System.err.println(e);
					}
				}
			}));
		}
		predecessor = address;
	}

	public static synchronized void pathEndCount(String address){
		String successor = address;
		String edge = predecessor + "-" + successor;
		if(predecessor != null){
			Long count = pathCounts.remove(edge);
			if(count == null){
				count = 1L;
			} else {
				count++;
			}
			pathCounts.put(edge, count);
		}
	}

	private static Map<String,List<List<Long>>> loopIterationTimes = Collections.synchronizedMap(new HashMap<String,List<List<Long>>>());
	
	public static synchronized void tick(String address){
		if(!initialized){
			initialized = true;
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						if(!abortHooks) {
							FileWriter fw = new FileWriter(new File("sidis.loop-times.dat"));
							for(Entry<String,List<List<Long>>> entry : loopIterationTimes.entrySet()){
								fw.write(entry.getKey() + ":" + entry.getValue() + "\n");
							}
							fw.flush();
							fw.close();
						}
					} catch (IOException e){
						System.err.println(e);
					}
				}
			}));
		}
		
		List<List<Long>> instances = loopIterationTimes.get(address);
		if(instances == null){
			instances = new LinkedList<List<Long>>();
			List<Long> instance = new LinkedList<Long>();
			instances.add(instance);
			loopIterationTimes.put(address, instances);
		}
		List<Long> instance = instances.get(instances.size()-1);
		instance.add(System.nanoTime());
	}
	
	public static synchronized void terminate(String address){
		List<List<Long>> instances = loopIterationTimes.get(address);
		if(instances != null){
			// if its null the loop path was not taken before
			// terminator was executed no big deal, just move on
			instances.add(new LinkedList<Long>());
		}
	}
	
	private static Map<String,Long> counts = Collections.synchronizedMap(new HashMap<String,Long>());
	
	public static synchronized void count(String address){
		if(!initialized){
			initialized = true;
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						if(!abortHooks) {
							int version = 1;
							File output = new File("sidis.ec." + version + ".dat");
							while(output.exists()) {
								version++;
								output = new File("sidis.ec." + version + ".dat");
							}
							FileWriter fw = new FileWriter(output);
							for(Entry<String,Long> entry : counts.entrySet()){
								fw.write(entry.getKey() + ":" + entry.getValue() + "\n");
							}
							fw.flush();
							fw.close();
						}
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
	
	public static synchronized void printCardinality(String address, Object o){
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

	public static synchronized void println(String value){
		if(out == null){
			out = getPrintStream();;
		}
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
