package com.kcsl.sidis.sid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.bytecode.JarModifier;
import com.ensoftcorp.open.java.commons.bytecode.JarPreservation;
import com.ensoftcorp.open.jimple.commons.soot.SootConversionException;
import com.ensoftcorp.open.jimple.commons.soot.Transformation;
import com.ensoftcorp.open.jimple.commons.soot.transforms.MethodCFGTransform;
import com.kcsl.sidis.Activator;

public class CFGTransformer {

	public static void transform(File jar, File outputJar, File libraryDirectory, boolean allowPhantomReferences, boolean generateClassFiles, MethodCFGTransform cfgTransform) throws IOException, CoreException, URISyntaxException, SootConversionException {

		File supportJar = null;
		File supportedJar = null;
		File instrumentedBytecodeJar = null;
		
		try {
			// if a previous version is already there, delete it now
	    	if(outputJar.exists()){
	    		outputJar.delete();
	    	}
	    	
	    	// add probe instrumentation support classes
			// load the instrumentation classes
	    	// see http://stackoverflow.com/q/23825933/475329 for logic of getting bundle resource
			URL fileURL = Activator.getDefault().getBundle().getEntry(Activator.INSTRUMENTS_JAR_PATH);
			URL resolvedFileURL = FileLocator.toFileURL(fileURL);
			// need to use the 3-arg constructor of URI in order to properly escape file system chars
			URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
			InputStream supportJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
			if(supportJarInputStream == null){
				throw new RuntimeException("Could not locate: " + Activator.INSTRUMENTS_JAR_PATH);
			}
			supportJar = File.createTempFile("instruments", ".jar");
			supportJar.delete(); // just need the temp file path
			Files.copy(supportJarInputStream, supportJar.toPath());
			
			JarInspector supportJarInspector = new JarInspector(supportJar);
			JarModifier supportedJarModifier = new JarModifier(jar);
			for(String entry : supportJarInspector.getJarEntrySet()){
				if(entry.endsWith(".class")){
					supportedJarModifier.add(entry, supportJarInspector.extractEntry(entry), true);
				}
			}
			
			supportedJar = File.createTempFile("supported", ".jar");
			supportedJarModifier.save(supportedJar);
	    	
	    	// add the default JVM classpath (assuming translator uses the same jvm libraries)
			List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
			IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
			LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
			for (LibraryLocation library : locations) {
				classpathEntries.add(JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null));
			}
			
			// convert classpath entries to files
			List<File> libraries = new ArrayList<File>();
			for(IClasspathEntry entry : classpathEntries){
				libraries.add(new File(entry.getPath().toFile().getCanonicalPath()));
			}
	    	
	    	// add any additional library dependencies
	    	if(libraryDirectory != null){
	    		libraries.add(libraryDirectory);
	    	}
	    	
	    	// create a temp file to hold the resulting jar file
	    	instrumentedBytecodeJar = File.createTempFile(outputJar.getName(), ".jar");
	    	instrumentedBytecodeJar.delete(); // just want the file handle
	    	
	    	// transform the supported jar
	    	boolean useOriginalNames = false;
			boolean outputBytecode = true;
	    	Transformation.transform(supportedJar, instrumentedBytecodeJar, libraries, allowPhantomReferences, useOriginalNames, outputBytecode, cfgTransform.getTransform());

			// copy the jar resources and sanitized manifest from the original bytecode
	    	JarPreservation.copyJarResources(jar, instrumentedBytecodeJar, outputJar);
		} finally {
			if(supportJar != null){
				supportJar.delete();
			}
			if(supportedJar != null){
				supportedJar.delete();
			}
			if(instrumentedBytecodeJar != null){
				instrumentedBytecodeJar.delete();
			}
		}
	}
	
}
