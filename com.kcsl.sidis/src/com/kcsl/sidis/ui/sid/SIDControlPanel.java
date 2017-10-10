package com.kcsl.sidis.ui.sid;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.ui.selection.IAtlasSelectionListener;
import com.ensoftcorp.atlas.ui.selection.SelectionUtil;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.jimple.commons.transform.Compilation;

public class SIDControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.sid.controlpanel"; //$NON-NLS-1$

	// the current Atlas selection
	private AtlasSet<Node> selection = new AtlasHashSet<Node>();

	private static Map<String,SIDExperiment> experiments = new HashMap<String,SIDExperiment>();
	private static SIDControlPanel VIEW;
	
	private static boolean initialized = false;
	private static int experimentCounter = 1;
	
	private CTabFolder experimentFolder;
	private Text transformationNameText;
	
	public SIDControlPanel() {
		setPartName("SID Control Panel");
		setTitleImage(ResourceManager.getPluginImage("com.kcsl.sidis", "icons/toolbox.gif"));
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		experimentFolder = new CTabFolder(parent, SWT.CLOSE);
		experimentFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		experimentFolder.setBorderVisible(true);
		experimentFolder.setSimple(false); // adds the Eclipse style "swoosh"
		
		// add a prompt to ask if we should really close the builder tab
		experimentFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				MessageBox messageBox = new MessageBox(Display.getCurrent().getActiveShell(),
						SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				String tabName = experimentFolder.getSelection().getText();
				messageBox.setMessage("Close SID [" + tabName + "] instance?");
				messageBox.setText("Closing Tab");
				int response = messageBox.open();
				if (response == SWT.YES) {
					experiments.remove(tabName);
				} else {
					event.doit = false;
				}
			}
		});
		
		// uncomment to preview with window builder
		SIDExperiment testExperiment = new SIDExperiment("TEST");
		experiments.put("TEST", testExperiment);
		addExperiment(experimentFolder, testExperiment);
		
		// create a new experiment if this is the first launch
		if(!initialized){
			int SID_EXPERIMENT_NUMBER = (experimentCounter++);
			String SID_EXPERIMENT_NAME = getUniqueName("Experiment " + SID_EXPERIMENT_NUMBER);
			SIDExperiment experiment = new SIDExperiment(SID_EXPERIMENT_NAME);
			experiments.put(SID_EXPERIMENT_NAME, experiment);
			addExperiment(experimentFolder, experiment);
			initialized = true;
		} else {
			// otherwise load what is already in memory
			ArrayList<SIDExperiment> sortedExperiments = new ArrayList<SIDExperiment>(experiments.values());
			Collections.sort(sortedExperiments); // sorted by creation time
			for(SIDExperiment experiment : sortedExperiments){
				addExperiment(experimentFolder, experiment);
			}
		}
		
		// add an add experiment tab button to the action bar
		final Action addExperimentAction = new Action() {
			public void run() {
				int SID_EXPERIMENT_NUMBER = (experimentCounter++);
				String SID_EXPERIMENT_NAME = getUniqueName("Experiment " + SID_EXPERIMENT_NUMBER);
				SIDExperiment experiment = new SIDExperiment(SID_EXPERIMENT_NAME);
				experiments.put(SID_EXPERIMENT_NAME, experiment);
				addExperiment(experimentFolder, experiment);
			}
		};
		
		addExperimentAction.setText("New SID Experiment");
		addExperimentAction.setToolTipText("Creates another SID experiment tab");
		ImageDescriptor newConfigurationIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.kcsl.sidis", "icons/new_configuration_button.png"));
		addExperimentAction.setImageDescriptor(newConfigurationIcon);
		addExperimentAction.setDisabledImageDescriptor(newConfigurationIcon);
		addExperimentAction.setHoverImageDescriptor(newConfigurationIcon);
		getViewSite().getActionBars().getToolBarManager().add(addExperimentAction);
		
		// setup the Atlas selection event listener
		IAtlasSelectionListener selectionListener = new IAtlasSelectionListener(){
			@Override
			public void selectionChanged(IAtlasSelectionEvent atlasSelection) {
				try {
					selection = atlasSelection.getSelection().eval().nodes();
				} catch (Exception e){
					selection = new AtlasHashSet<Node>();
				}
			}				
		};
		
		// add the selection listener
		SelectionUtil.addSelectionListener(selectionListener);
	}
	
	private void addExperiment(final CTabFolder experimentFolder, final SIDExperiment experiment) {
		final CTabItem experimentTab = new CTabItem(experimentFolder, SWT.NONE);
		experimentTab.setText(experiment.getName());
		
		Composite experimentComposite = new Composite(experimentFolder, SWT.NONE);
		experimentTab.setControl(experimentComposite);
		experimentComposite.setLayout(new GridLayout(1, false));
		
		Composite experimentControlPanelComposite = new Composite(experimentComposite, SWT.NONE);
		experimentControlPanelComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		experimentControlPanelComposite.setLayout(new GridLayout(2, false));
		
		Label experimentNameLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		experimentNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		experimentNameLabel.setSize(66, 14);
		experimentNameLabel.setText("Experiment Name: ");
		
		Composite experimentLabelComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		experimentLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		experimentLabelComposite.setLayout(new GridLayout(1, false));
		
		final Text experimentLabelText = new Text(experimentLabelComposite, SWT.BORDER);
		experimentLabelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		experimentLabelText.setText(experiment.getName());
		
		experimentLabelText.addTraverseListener(new TraverseListener(){
			@Override
			public void keyTraversed(TraverseEvent event) {
				if(event.detail == SWT.TRAVERSE_RETURN){
					String newName = experimentLabelText.getText();
					experimentTab.setText(newName);
					experiment.setName(newName);
				}
			}
		});
		
		Label workspaceProjectLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		workspaceProjectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		workspaceProjectLabel.setText("Workspace Project: ");
		
		Composite workspaceComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		workspaceComposite.setLayout(new GridLayout(1, false));
		workspaceComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Combo workspaceProjectCombo = new Combo(workspaceComposite, SWT.NONE);
		workspaceProjectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		workspaceProjectCombo.removeAll();
		LinkedList<IProject> projects = getWorkspaceProjects();
		for(IProject project : projects){
			workspaceProjectCombo.add(project.getName());
			workspaceProjectCombo.setData(project.getName(), project);
		}
		
		Label jimpleDirectoryLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		jimpleDirectoryLabel.setText("Jimple Directory: ");
		
		Composite jimpleDirectoryComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		jimpleDirectoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		jimpleDirectoryComposite.setLayout(new GridLayout(2, false));
		
		Button browseJimpleDirectoryButton = new Button(jimpleDirectoryComposite, SWT.NONE);
		browseJimpleDirectoryButton.setText("Browse...");
		
		Label jimpleDirectoryPathLabel = new Label(jimpleDirectoryComposite, SWT.NONE);
		jimpleDirectoryPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label originalBytecodeLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		originalBytecodeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		originalBytecodeLabel.setText("Original Bytecode: ");
		
		Composite orginalBytecodeComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		orginalBytecodeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		orginalBytecodeComposite.setLayout(new GridLayout(2, false));
		
		Button browseOriginalBytecodeButton = new Button(orginalBytecodeComposite, SWT.NONE);
		browseOriginalBytecodeButton.setText("Browse...");
		
		Label originalBytecodePathLabel = new Label(orginalBytecodeComposite, SWT.NONE);
		originalBytecodePathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label label = new Label(experimentComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		SashForm sashForm = new SashForm(experimentComposite, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite transformationConfigurationComposite = new Composite(sashForm, SWT.NONE);
		transformationConfigurationComposite.setLayout(new GridLayout(1, false));
		
		Composite transformationSelectionComposite = new Composite(transformationConfigurationComposite, SWT.NONE);
		transformationSelectionComposite.setLayout(new GridLayout(2, false));
		transformationSelectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label transformationLabel = new Label(transformationSelectionComposite, SWT.NONE);
		transformationLabel.setText("Transformation: ");
		
		Combo transformationCombo = new Combo(transformationSelectionComposite, SWT.NONE);
		transformationCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Group transformationConfigurationGroup = new Group(transformationConfigurationComposite, SWT.NONE);
		transformationConfigurationGroup.setText("Transformation Configurations");
		transformationConfigurationGroup.setLayout(new GridLayout(1, false));
		transformationConfigurationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite transformationNameComposite = new Composite(transformationConfigurationGroup, SWT.NONE);
		transformationNameComposite.setLayout(new GridLayout(2, false));
		transformationNameComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label transformationNameLabel = new Label(transformationNameComposite, SWT.NONE);
		transformationNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		transformationNameLabel.setText("Name: ");
		
		transformationNameText = new Text(transformationNameComposite, SWT.BORDER);
		transformationNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Composite transformationConfigurationParametersComposite = new Composite(transformationConfigurationGroup, SWT.NONE);
		transformationConfigurationParametersComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite composite = new Composite(transformationConfigurationGroup, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button saveTransformationButton = new Button(composite, SWT.NONE);
		saveTransformationButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		saveTransformationButton.setText("Save");
		
		Group appliedTransformationsGroup = new Group(sashForm, SWT.NONE);
		appliedTransformationsGroup.setLayout(new GridLayout(1, false));
		appliedTransformationsGroup.setText("Bytecode Transformations");
		
		List bytecodeTransformationList = new List(appliedTransformationsGroup, SWT.BORDER);
		bytecodeTransformationList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite appliedTransformationsControlsComposite = new Composite(appliedTransformationsGroup, SWT.NONE);
		appliedTransformationsControlsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		appliedTransformationsControlsComposite.setLayout(new GridLayout(2, false));
		
		Button deleteTransformationButton = new Button(appliedTransformationsControlsComposite, SWT.NONE);
		deleteTransformationButton.setText("Delete Transformation");
		
		Button generateBytecodeButton = new Button(appliedTransformationsControlsComposite, SWT.NONE);
		generateBytecodeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		generateBytecodeButton.setText("Generate Bytecode");
		sashForm.setWeights(new int[] {1, 1});
		
		// set the default project, if there is only one
		if(workspaceProjectCombo.getItemCount() == 1){
			workspaceProjectCombo.select(0);
			setExperimentProject(experiment, projects.getFirst(), jimpleDirectoryPathLabel);
		}
		
		browseJimpleDirectoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		        dd.setText("Open Jimple Directory");
		        if(experiment.getProject() != null){
		        	dd.setFilterPath(experiment.getProject().getLocation().toFile().getAbsolutePath());
		        } else {
		        	// just set it to user home directory
		        	dd.setFilterPath(System.getProperty("user.home"));
		        }
		        String path = dd.open();
		        if(path != null){
		        	File jimpleDirectory = new File(path);
			        experiment.setJimpleDirectory(jimpleDirectory);
			        jimpleDirectoryPathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
					jimpleDirectoryPathLabel.setText(path);
		        }
			}
		});
		
		workspaceProjectCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject project = (IProject) workspaceProjectCombo.getData(workspaceProjectCombo.getText());
				setExperimentProject(experiment, project, jimpleDirectoryPathLabel);
			}
		});
		
		browseOriginalBytecodeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		        fd.setText("Select Original Bytecode");
		        String[] filterExt = { "*.jar", "*.*" };
		        fd.setFilterExtensions(filterExt);
		        if(experiment.getProject() != null){
		        	fd.setFilterPath(experiment.getProject().getLocation().toFile().getAbsolutePath());
		        } else {
		        	// just set it to user home directory
		        	fd.setFilterPath(System.getProperty("user.home"));
		        }
		        String path = fd.open();
		        if(path != null){
		        	File originalBytecode = new File(path);
			        experiment.setOriginalBytecode(originalBytecode);
			        originalBytecodePathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			        originalBytecodePathLabel.setText(path);
		        }
			}
		});
		
		transformationCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
			}
		});
		
		// set the tab selection to this newly created tab
		experimentFolder.setSelection(experimentFolder.getItemCount()-1);
	}

	@Override
	public void setFocus() {
		// intentionally left blank
	}
	
	private void setExperimentProject(final SIDExperiment experiment, IProject project, Label jimpleDirectoryPathLabel) {
		experiment.setProject(project);
		try {
			experiment.setJimpleDirectory(Compilation.getJimpleDirectory(project.getLocation().toFile()));
		} catch (Throwable t) {
			jimpleDirectoryPathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
			jimpleDirectoryPathLabel.setText("Unable to auto locate Jimple directory.");
		}
	}
	
	private void setFocus(SIDExperiment experiment){
		int index = 0;
		ArrayList<SIDExperiment> sortedExperiments = new ArrayList<SIDExperiment>(experiments.values());
		Collections.sort(sortedExperiments); // sorted by creation time
		for(SIDExperiment sortedExperiment : sortedExperiments){
			if(experiment.equals(sortedExperiment)){
				break;
			} else {
				index++;
			}
		}
		if(index <= experimentFolder.getItemCount()-1){
			experimentFolder.setSelection(index);
		}
	}
	
	private static LinkedList<IProject> getWorkspaceProjects(){
		LinkedList<IProject> projects = new LinkedList<IProject>();
		for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
			if(project.isOpen()){
				// TODO: consider cross referencing with projects in Atlas index
				projects.add(project);
			}
		}
		Collections.sort(projects, new Comparator<IProject>(){
			@Override
			public int compare(IProject p1, IProject p2) {
				return p1.getName().compareTo(p2.getName());
			}
		});
		return projects;
	}
	
	private static String getUniqueName(String experimentName){
		int suffix = 2;
		while(experiments.containsKey(experimentName)){
			experimentName = experimentName + " " + (suffix++);
		}
		return experimentName;
	}
}
