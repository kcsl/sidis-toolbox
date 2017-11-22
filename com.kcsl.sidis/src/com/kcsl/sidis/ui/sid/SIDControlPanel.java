package com.kcsl.sidis.ui.sid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
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
import com.ensoftcorp.atlas.core.indexing.IIndexListener;
import com.ensoftcorp.atlas.core.indexing.IndexingUtil;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.selection.IAtlasSelectionListener;
import com.ensoftcorp.atlas.ui.selection.SelectionUtil;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.ensoftcorp.open.commons.utilities.DisplayUtils;
import com.ensoftcorp.open.commons.utilities.NodeSourceCorrespondenceSorter;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;
import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.bytecode.JarModifier;
import com.ensoftcorp.open.jimple.commons.transform.Compilation;
import com.kcsl.sidis.Activator;
import com.kcsl.sidis.log.Log;
import com.kcsl.sidis.sid.instruments.Probe;

import soot.Transform;

public class SIDControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.sid.controlpanel"; //$NON-NLS-1$
	
	public static final String INSTRUMENTS_ZIP_PATH = "instruments/instruments.zip";

	// the current Atlas selection
	private AtlasSet<Node> selection = new AtlasHashSet<Node>();

	private static Map<String,SIDExperiment> experiments = new HashMap<String,SIDExperiment>();
//	private static SIDControlPanel VIEW;
	
	private static boolean initialized = false;
	private static int experimentCounter = 1;
	private static boolean saveIndexReminder = true;
	
	private CTabFolder experimentFolder;
	
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
		
		IndexingUtil.addListener(new IIndexListener(){
			@Override
			public void indexOperationCancelled(IndexOperation op) {}

			@Override
			public void indexOperationComplete(IndexOperation op) {
				saveIndexReminder = true;
			}

			@Override
			public void indexOperationError(IndexOperation op, Throwable error) {}

			@Override
			public void indexOperationScheduled(IndexOperation op) {}

			@Override
			public void indexOperationStarted(IndexOperation op) {
				saveIndexReminder = true;
			}
		});
		
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
//		SIDExperiment testExperiment = new SIDExperiment("TEST");
//		experiments.put("TEST", testExperiment);
//		addExperiment(experimentFolder, testExperiment);
		
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
		
		ExpandBar compilerConfigurationsExpandBar = new ExpandBar(experimentComposite, SWT.NONE);
		compilerConfigurationsExpandBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		ExpandItem compilerConfigurationsExpandItem = new ExpandItem(compilerConfigurationsExpandBar, SWT.NONE);
		compilerConfigurationsExpandItem.setExpanded(true);
		compilerConfigurationsExpandItem.setText("Compiler Configurations");
		
		Composite experimentControlPanelComposite = new Composite(compilerConfigurationsExpandBar, SWT.NONE);
		compilerConfigurationsExpandItem.setControl(experimentControlPanelComposite);
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
		
		Label jimpleDirectoryLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		jimpleDirectoryLabel.setText("Jimple Directory: ");
		
		Composite jimpleDirectoryComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		jimpleDirectoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		jimpleDirectoryComposite.setLayout(new GridLayout(2, false));
		
		Button browseJimpleDirectoryButton = new Button(jimpleDirectoryComposite, SWT.NONE);
		browseJimpleDirectoryButton.setText("Browse...");
		
		Label jimpleDirectoryPathLabel = new Label(jimpleDirectoryComposite, SWT.NONE);
		jimpleDirectoryPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label libraryDirectoryLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		libraryDirectoryLabel.setText("Library Directory: ");
		
		Composite libraryDirectoryComposite = new Composite(experimentControlPanelComposite, SWT.NONE);
		libraryDirectoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		libraryDirectoryComposite.setLayout(new GridLayout(2, false));
		
		Button browseLibraryDirectoryButton = new Button(libraryDirectoryComposite, SWT.NONE);
		browseLibraryDirectoryButton.setText("Browse...");
		
		Label libraryDirectoryPathLabel = new Label(libraryDirectoryComposite, SWT.NONE);
		libraryDirectoryPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
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
									
		compilerConfigurationsExpandItem.setHeight(compilerConfigurationsExpandItem.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		
		compilerConfigurationsExpandBar.addExpandListener(new ExpandAdapter() {
			
			private static final int EXPAND_BAR_SIZE = 32;
			
			@Override
			public void itemExpanded(ExpandEvent e) {
				compilerConfigurationsExpandBar.setSize(compilerConfigurationsExpandBar.getSize().x, compilerConfigurationsExpandItem.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y + EXPAND_BAR_SIZE);
				compilerConfigurationsExpandBar.requestLayout();
			}
			@Override
			public void itemCollapsed(ExpandEvent e) {
				compilerConfigurationsExpandBar.setSize(compilerConfigurationsExpandBar.getSize().x, EXPAND_BAR_SIZE);
				compilerConfigurationsExpandBar.requestLayout();
			}
		});
		
		LinkedList<IProject> projects = getWorkspaceProjects();
		for(IProject project : projects){
			workspaceProjectCombo.add(project.getName());
			workspaceProjectCombo.setData(project.getName(), project);
		}
		
		Label label = new Label(experimentComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		
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
		transformationCombo.setItems("Statement Counter Probe");
		transformationCombo.select(0);
		transformationCombo.setEnabled(false);
		
		Group transformationConfigurationGroup = new Group(transformationConfigurationComposite, SWT.NONE);
		transformationConfigurationGroup.setText("Transformation Configurations");
		transformationConfigurationGroup.setLayout(new GridLayout(1, false));
		transformationConfigurationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Group statementCounterProbesGroup = new Group(transformationConfigurationGroup, SWT.NONE);
		statementCounterProbesGroup.setText("Statement Counter Probes: 0");
		statementCounterProbesGroup.setLayout(new GridLayout(2, false));
		statementCounterProbesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Label methodsLabel = new Label(statementCounterProbesGroup, SWT.NONE);
		methodsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		methodsLabel.setText("Methods: 0");
		
		Label statementsLabel = new Label(statementCounterProbesGroup, SWT.NONE);
		statementsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		statementsLabel.setText("Statements: 0");
		
		List methodsList = new List(statementCounterProbesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		methodsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		List statementsList = new List(statementCounterProbesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		statementsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		refreshMethodProbeLists(experiment, methodsList, methodsLabel, statementsList, statementsLabel);
		
		Composite transformationConfigurationParametersComposite = new Composite(transformationConfigurationGroup, SWT.NONE);
		transformationConfigurationParametersComposite.setLayout(new GridLayout(1, false));
		transformationConfigurationParametersComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Group graphSelectionsGroup = new Group(transformationConfigurationParametersComposite, SWT.NONE);
		graphSelectionsGroup.setText("Graph Selections");
		graphSelectionsGroup.setLayout(new GridLayout(2, false));
		graphSelectionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Button addSelectedButton = new Button(graphSelectionsGroup, SWT.NONE);
		addSelectedButton.setText("Add Selected");
		
		Button deleteSelectedButton = new Button(graphSelectionsGroup, SWT.NONE);
		deleteSelectedButton.setText("Delete Selected");
		
		Group appliedTransformationsGroup = new Group(sashForm, SWT.NONE);
		appliedTransformationsGroup.setLayout(new GridLayout(1, false));
		appliedTransformationsGroup.setText("Bytecode Transformations");
		
		List bytecodeTransformationList = new List(appliedTransformationsGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		bytecodeTransformationList.setItems(new String[] {"Statement Counter Probes: 0"});
		bytecodeTransformationList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite appliedTransformationsControlsComposite = new Composite(appliedTransformationsGroup, SWT.NONE);
		appliedTransformationsControlsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		appliedTransformationsControlsComposite.setLayout(new GridLayout(2, false));
		
		Button debugCheckbox = new Button(appliedTransformationsControlsComposite, SWT.CHECK);
		debugCheckbox.setText("Debug");
		
		Button generateBytecodeButton = new Button(appliedTransformationsControlsComposite, SWT.NONE);
		generateBytecodeButton.setEnabled(false);
		generateBytecodeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		generateBytecodeButton.setText("Generate Bytecode");
		sashForm.setWeights(new int[] {600, 301});
		
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
		        validateGenerateBytecodeButton(experiment, generateBytecodeButton);
			}
		});
		
		browseLibraryDirectoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		        dd.setText("Open Library Directory");
		        if(experiment.getProject() != null){
		        	dd.setFilterPath(experiment.getProject().getLocation().toFile().getAbsolutePath());
		        } else {
		        	// just set it to user home directory
		        	dd.setFilterPath(System.getProperty("user.home"));
		        }
		        String path = dd.open();
		        if(path != null){
		        	File libraryDirectory = new File(path);
			        experiment.setLibraryDirectory(libraryDirectory);
			        libraryDirectoryPathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
					libraryDirectoryPathLabel.setText(path);
		        }
		        validateGenerateBytecodeButton(experiment, generateBytecodeButton);
			}
		});
		
		workspaceProjectCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject project = (IProject) workspaceProjectCombo.getData(workspaceProjectCombo.getText());
				setExperimentProject(experiment, project, jimpleDirectoryPathLabel, generateBytecodeButton);
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
		
		// set the default project, if there is only one
		if(workspaceProjectCombo.getItemCount() == 1){
			workspaceProjectCombo.select(0);
			setExperimentProject(experiment, projects.getFirst(), jimpleDirectoryPathLabel, generateBytecodeButton);
		}
		
		methodsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if(methodsList.getSelectionCount() == 1){
					String name = methodsList.getSelection()[0];
					Node method = (Node) methodsList.getData(name);
					DisplayUtils.show(method, CommonQueries.getQualifiedMethodName(method));
				}
			}
		});
		
		statementsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if(methodsList.getSelectionCount() == 1){
					String name = statementsList.getSelection()[0];
					Node statement = (Node) statementsList.getData(statementsList.getSelectionIndex() + name);
					DisplayUtils.show(Common.toQ(statement), CommonQueries.getQualifiedName(statement));
				}
			}
		});
		
		methodsList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(methodsList.getSelectionCount() == 1){
					String name = methodsList.getSelection()[0];
					Node method = (Node) methodsList.getData(name);
					StatementCounterProbeRequest request = experiment.getStatementCounterProbeRequest();
					statementsList.removeAll();
					AtlasSet<Node> statements = request.getRequestMethodStatements(method);
					ArrayList<Node> sortedStatements = new ArrayList<Node>((int) statements.size());
					for(Node statement : statements){
						sortedStatements.add(statement);
					}
					Collections.sort(sortedStatements, new NodeSourceCorrespondenceSorter());
					int index = 0; // qualifying key with list index because statements could have colliding names
					for(Node statement : sortedStatements){
						String statementName = statement.getAttr(XCSG.name).toString();
						statementsList.add(statementName);
						statementsList.setData((index++) + statementName, statement);
					}
					statementsLabel.setText("Statements: " + sortedStatements.size());
				}
			}
		});
		
		addSelectedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AtlasSet<Node> statements = selection.taggedWithAny(XCSG.ControlFlow_Node);
				AtlasSet<Node> statementMethods = CommonQueries.getContainingMethods(Common.toQ(statements)).eval().nodes();
				AtlasSet<Node> methods = selection.taggedWithAny(XCSG.Method);
				methods = Common.toQ(methods).difference(Common.toQ(statementMethods)).eval().nodes();
				
				StatementCounterProbeRequest request = experiment.getStatementCounterProbeRequest();
				request.addAllStatementProbes(methods);
				for(Node statementMethod : statementMethods){
					request.addStatementProbes(statementMethod, CommonQueries.cfg(statementMethod).intersection(Common.toQ(statements)).eval().nodes());
				}
				
				refreshMethodProbeLists(experiment, methodsList, methodsLabel, statementsList, statementsLabel);
				refreshTotalProbesCount(request.getTotalStatementProbeRequests(), statementCounterProbesGroup, bytecodeTransformationList);
				validateGenerateBytecodeButton(experiment, generateBytecodeButton);
			}
		});
		
		deleteSelectedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AtlasSet<Node> statements = selection.taggedWithAny(XCSG.ControlFlow_Node);
				AtlasSet<Node> statementMethods = CommonQueries.getContainingMethods(Common.toQ(statements)).eval().nodes();
				AtlasSet<Node> methods = selection.taggedWithAny(XCSG.Method);
				methods = Common.toQ(methods).difference(Common.toQ(statementMethods)).eval().nodes();
				
				StatementCounterProbeRequest request = experiment.getStatementCounterProbeRequest();
				request.removeAllStatementProbes(methods);
				for(Node statementMethod : statementMethods){
					request.removeStatementProbes(statementMethod, CommonQueries.cfg(statementMethod).intersection(Common.toQ(statements)).eval().nodes());
				}
				
				refreshMethodProbeLists(experiment, methodsList, methodsLabel, statementsList, statementsLabel);
				refreshTotalProbesCount(request.getTotalStatementProbeRequests(), statementCounterProbesGroup, bytecodeTransformationList);
				validateGenerateBytecodeButton(experiment, generateBytecodeButton);
			}
		});
		
		generateBytecodeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		        fd.setText("Save Transformed Bytecode");
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
		        	File generatedBytecode = new File(path);
		        	
		        	// if a previous version is already there, delete it now
		        	if(generatedBytecode.exists()){
		        		generatedBytecode.delete();
		        	}
		        	
		        	boolean allowPhantomReferences = true;
		        	boolean generateClassFiles = !debugCheckbox.getSelection();
		        	ArrayList<Transform> probeTransforms = new ArrayList<Transform>();
		        	for(Probe probe : experiment.getStatementCounterProbeRequest().getProbes()){
		        		probeTransforms.add(probe.getTransform());
		        	}
		        	final Transform[] transforms = new Transform[probeTransforms.size()];
		        	probeTransforms.toArray(transforms);
		        	
			        try {
			        	// create a temp file to hold all the jimple code in a flat directory
			        	File tmpJimpleDirectory = Files.createTempDirectory("jimple_").toFile();
			        	tmpJimpleDirectory.mkdirs();
			        	for(File jimpleFile : Compilation.findJimple(experiment.getJimpleDirectory())){
			        		FileUtils.copyFile(jimpleFile, new File(tmpJimpleDirectory.getAbsolutePath() + File.separator + jimpleFile.getName()));
			        	}
			        	
			        	// load the instrumentation classes
			        	// see http://stackoverflow.com/q/23825933/475329 for logic of getting bundle resource
			    		URL fileURL = Activator.getDefault().getBundle().getEntry(INSTRUMENTS_ZIP_PATH);
			    		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
			    		// need to use the 3-arg constructor of URI in order to properly escape file system chars
			    		URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
			    		InputStream annotationsJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
			    		if(annotationsJarInputStream == null){
			    			throw new RuntimeException("Could not locate: " + INSTRUMENTS_ZIP_PATH);
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
			        	File tmpOutputBytecode = File.createTempFile(generatedBytecode.getName(), ".jar");
			        	
			        	// generate bytecode for jimple
			        	LinkedList<File> libraries = new LinkedList<File>();
			        	if(experiment.getLibraryDirectory() != null){
			        		libraries.add(experiment.getLibraryDirectory());
			        	}
						Compilation.compile(experiment.getProject(), tmpJimpleDirectory, tmpOutputBytecode, allowPhantomReferences, libraries, generateClassFiles, transforms);
						
						// clean up temp directory
						try {
							FileUtils.deleteDirectory(tmpJimpleDirectory);
						} catch (IOException ioe){
							// don't care if it fails, its in a temp directory anyway, OS will take care of it
						}
						
						// if applicable copy the jar resources and sanitized manifest from the original bytecode
						if(experiment.getOriginalBytecode() != null){
							JarInspector inspector = new JarInspector(experiment.getOriginalBytecode());
							JarModifier modifier = new JarModifier(tmpOutputBytecode);
							// copy over the original jar resources
							for(String entry : inspector.getJarEntrySet()){
								if(!entry.endsWith(".class")){
									byte[] bytes = inspector.extractEntry(entry);
									modifier.add(entry, bytes, true);
								}
							}
							modifier.save(generatedBytecode);
						} else {
							tmpOutputBytecode.renameTo(generatedBytecode);
						}
//						if(saveIndexReminder){
//							saveIndexReminder = false;
//							if(DisplayUtils.promptBoolean("Save Codemap", "This generated bytecode contains instrumentation that is tied to your current Atlas codemap. Would you like to save your code map now?")){
//								FileDialog fdIndex = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
//						        fdIndex.setText("Save Atlas Index");
//						        String[] filterExtIndex = { "*.atlas", "*.*" };
//						        fdIndex.setFilterExtensions(filterExtIndex);
//						        if(experiment.getProject() != null){
//						        	fdIndex.setFilterPath(experiment.getProject().getLocation().toFile().getAbsolutePath());
//						        } else {
//						        	// just set it to user home directory
//						        	fdIndex.setFilterPath(System.getProperty("user.home"));
//						        }
//						        String indexSavePath = fdIndex.open();
//						        if(indexSavePath != null){
//						        	IndexingUtil.saveIndex(indexSavePath, false);
//						        }
//							}
//						}
					} catch (Throwable t) {
						String message = "Error compiling transformed bytecode.";
						Log.error(message, t);
						DisplayUtils.showError(t, message);
					}
		        }
			}
		});
		
		// set the tab selection to this newly created tab
		experimentFolder.setSelection(experimentFolder.getItemCount()-1);
	}
	
	private void refreshTotalProbesCount(int totalProbes, Group statementCounterProbesGroup, List bytecodeTransformationList){
		statementCounterProbesGroup.setText("Statement Counter Probes: " + totalProbes);
		bytecodeTransformationList.setItems(new String[] {"Statement Counter Probes: " + totalProbes});
	}

	private void refreshMethodProbeLists(final SIDExperiment experiment, List methodsList, Label methodsLabel, List statementsList, Label statementsLabel) {
		StatementCounterProbeRequest request = experiment.getStatementCounterProbeRequest();
		methodsList.removeAll();
		statementsList.removeAll();
		AtlasSet<Node> methods = request.getRequestMethods();
		ArrayList<Node> sortedMethods = new ArrayList<Node>((int) methods.size());
		for(Node method : methods){
			sortedMethods.add(method);
		}
		Collections.sort(sortedMethods, new Comparator<Node>(){
			@Override
			public int compare(Node m1, Node m2) {
				String qm1 = CommonQueries.getQualifiedMethodName(m1);
				String qm2 = CommonQueries.getQualifiedMethodName(m2);
				return qm1.compareTo(qm2);
			}
		});
		for(Node method : sortedMethods){
			String name = CommonQueries.getQualifiedMethodName(method);
			methodsList.add(name);
			methodsList.setData(name, method);
		}
		methodsLabel.setText("Methods: " + methods.size());
		statementsLabel.setText("Statements: 0");
	}

	@Override
	public void setFocus() {
		// intentionally left blank
	}
	
	private void validateGenerateBytecodeButton(SIDExperiment experiment, Button generateBytecodeButton){
		if(experiment.getProject() == null){
			generateBytecodeButton.setEnabled(false);
			return;
		}
		if(experiment.getJimpleDirectory() == null){
			generateBytecodeButton.setEnabled(false);
			return;
		}
		if(experiment.getStatementCounterProbeRequest().getTotalStatementProbeRequests() == 0){
			generateBytecodeButton.setEnabled(false);
			return;
		}
		
		generateBytecodeButton.setEnabled(true);
	}
	
	private void setExperimentProject(SIDExperiment experiment, IProject project, Label jimpleDirectoryPathLabel, Button generateBytecodeButton) {
		experiment.setProject(project);
		try {
			File jimpleDirectoryPath = Compilation.getJimpleDirectory(project.getLocation().toFile());
			experiment.setJimpleDirectory(jimpleDirectoryPath);
			jimpleDirectoryPathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			jimpleDirectoryPathLabel.setText(jimpleDirectoryPath.getAbsolutePath());
		} catch (Throwable t) {
			experiment.setJimpleDirectory(null);
			jimpleDirectoryPathLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
			jimpleDirectoryPathLabel.setText("Unable to auto locate Jimple directory.");
		}
		validateGenerateBytecodeButton(experiment, generateBytecodeButton);
	}
	
//	private void setFocus(SIDExperiment experiment){
//		int index = 0;
//		ArrayList<SIDExperiment> sortedExperiments = new ArrayList<SIDExperiment>(experiments.values());
//		Collections.sort(sortedExperiments); // sorted by creation time
//		for(SIDExperiment sortedExperiment : sortedExperiments){
//			if(experiment.equals(sortedExperiment)){
//				break;
//			} else {
//				index++;
//			}
//		}
//		if(index <= experimentFolder.getItemCount()-1){
//			experimentFolder.setSelection(index);
//		}
//	}
	
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
