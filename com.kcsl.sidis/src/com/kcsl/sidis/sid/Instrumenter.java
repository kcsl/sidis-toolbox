package com.kcsl.sidis.sid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;

import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.bytecode.JarModifier;
import com.ensoftcorp.open.jimple.commons.transform.Compilation;
import com.kcsl.sidis.Activator;
import com.kcsl.sidis.log.Log;
import com.kcsl.sidis.sid.instruments.Probe;

import soot.Transform;

public class Instrumenter {

	public static void instrument(IProject project, File jimpleDirectory, File libraryDirectory, File originalBytecode, Set<Probe> probes, boolean allowPhantomReferences, boolean generateClassFiles, File output) throws IOException, CoreException, URISyntaxException{

    	// if a previous version is already there, delete it now
    	if(output.exists()){
    		output.delete();
    	}
    	
    	ArrayList<Transform> probeTransforms = new ArrayList<Transform>();
    	for(Probe probe : probes){
    		probeTransforms.add(probe.getTransform());
    	}
    	final Transform[] transforms = new Transform[probeTransforms.size()];
    	probeTransforms.toArray(transforms);
    	
    	// create a temp file to hold all the jimple code in a flat directory
    	File tmpJimpleDirectory = Files.createTempDirectory("jimple_").toFile();
    	tmpJimpleDirectory.mkdirs();
    	for(File jimpleFile : Compilation.findJimple(jimpleDirectory)){
    		FileUtils.copyFile(jimpleFile, new File(tmpJimpleDirectory.getAbsolutePath() + File.separator + jimpleFile.getName()));
    	}
    	
    	// load the instrumentation classes
    	// see http://stackoverflow.com/q/23825933/475329 for logic of getting bundle resource
		URL fileURL = Activator.getDefault().getBundle().getEntry(Activator.INSTRUMENTS_ZIP_PATH);
		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
		// need to use the 3-arg constructor of URI in order to properly escape file system chars
		URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
		InputStream annotationsJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
		if(annotationsJarInputStream == null){
			throw new RuntimeException("Could not locate: " + Activator.INSTRUMENTS_ZIP_PATH);
		}
		File instrumentsZip = File.createTempFile("instruments", ".zip");
		instrumentsZip.delete(); // just need the temp file path
		Files.copy(annotationsJarInputStream, instrumentsZip.toPath());
		
		// extract the instruments into the jimple directory
		FileInputStream fis;
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(instrumentsZip);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File instrument = new File(tmpJimpleDirectory.getAbsolutePath() + File.separator + fileName);
                File directory = new File(instrument.getParent());
                directory.mkdirs();
                FileOutputStream fos = new FileOutputStream(instrument);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException ioe) {
            Log.warning("Unable to load instruments.", ioe);
        }
        
    	// create a temp file to hold the resulting jar file
    	File tmpOutputBytecode = File.createTempFile(output.getName(), ".jar");
    	
    	// generate bytecode for jimple
    	LinkedList<File> libraries = new LinkedList<File>();
    	if(libraryDirectory != null){
    		libraries.add(libraryDirectory);
    	}
		Compilation.compile(project, tmpJimpleDirectory, tmpOutputBytecode, allowPhantomReferences, libraries, generateClassFiles, transforms);
		
		// clean up temp directory
		try {
			FileUtils.deleteDirectory(tmpJimpleDirectory);
		} catch (IOException ioe){
			// don't care if it fails, its in a temp directory anyway, OS will take care of it
		}
		
		// if applicable copy the jar resources and sanitized manifest from the original bytecode
		if(originalBytecode != null){
			JarInspector inspector = new JarInspector(originalBytecode);
			JarModifier modifier = new JarModifier(tmpOutputBytecode);
			// copy over the original jar resources
			for(String entry : inspector.getJarEntrySet()){
				if(!entry.endsWith(".class")){
					byte[] bytes = inspector.extractEntry(entry);
					modifier.add(entry, bytes, true);
				}
			}
			modifier.save(output);
		} else {
			tmpOutputBytecode.renameTo(output);
		}
	}
	
}
