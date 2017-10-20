package com.kcsl.sidis.sid;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarException;

import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.bytecode.JarModifier;

public class JarPreseveration {

	public static void preserveResources(File originalJar, File generatedJar, File outputJar) throws JarException, IOException {
		JarInspector inspector = new JarInspector(originalJar);
		JarModifier modifier = new JarModifier(generatedJar);
		// copy over the original jar resources
		for(String entry : inspector.getJarEntrySet()){
			if(!entry.endsWith(".class")){
				byte[] bytes = inspector.extractEntry(entry);
				modifier.add(entry, bytes, true);
			}
		}
		modifier.save(outputJar);
	}
	
}
