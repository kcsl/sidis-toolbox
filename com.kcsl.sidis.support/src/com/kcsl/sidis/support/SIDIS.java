package com.kcsl.sidis.support;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class SIDIS {

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
