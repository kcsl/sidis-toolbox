package com.kcsl.sidis.support;

import java.util.ArrayList;

import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

public class Support {

	/**
	 * Programmatically adds support classes to the application
	 * @param scene
	 */
	public static void generateSupportClasses(Scene scene){
		// load support classes
		scene.loadClassAndSupport("java.lang.Object");
		scene.loadClassAndSupport("java.io.File");
		scene.loadClassAndSupport("java.io.IOException");
		scene.loadClassAndSupport("java.io.PrintStream");
		
		// create a new soot class
		SootClass sidisSupportClass = new SootClass("com.kcsl.sidis.support.SIDIS", Modifier.PUBLIC);
		scene.addClass(sidisSupportClass);
		
		// add getPrintStream support method
		SootMethod getPrintStreamMethod = generateGetPrintStreamMethod(scene);
		sidisSupportClass.addMethod(getPrintStreamMethod);
	}

	// reference: https://github.com/Sable/soot/wiki/Creating-a-class-from-scratch
	private static SootMethod generateGetPrintStreamMethod(Scene scene) {
		Type printStreamType = scene.loadClassAndSupport("java.io.PrintStream").getType();
		SootMethod method = new SootMethod("getPrintStream", new ArrayList<Type>(), printStreamType, Modifier.PUBLIC | Modifier.STATIC);
		JimpleBody body = Jimple.v().newBody(method);

		// TODO: fill out method body
		
		method.setActiveBody(body);
		return method;
	}
	
}
