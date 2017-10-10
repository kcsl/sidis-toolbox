package com.kcsl.sidis.ui.sid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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

public class SIDControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.sid.controlpanel"; //$NON-NLS-1$

	// the current Atlas selection
	private AtlasSet<Node> selection = new AtlasHashSet<Node>();

	private static Map<String,SIDExperiment> experiments = new HashMap<String,SIDExperiment>();
	private static SIDControlPanel VIEW;
	
	private static boolean initialized = false;
	private static int experimentCounter = 1;
	
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
		
		experimentFolder = new CTabFolder(parent, SWT.CLOSE);
		experimentFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		experimentFolder.setBorderVisible(true);
		experimentFolder.setSimple(false); // adds the Eclipse style "swoosh"
		
		// add a prompt to ask if we should really close the builder tab
		experimentFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				MessageBox messageBox = new MessageBox(Display.getCurrent().getActiveShell(),
						SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage("Close SID experiment instance?");
				messageBox.setText("Closing Tab");
				int response = messageBox.open();
				if (response == SWT.YES) {
					String tabName = experimentFolder.getSelection().getText();
					experiments.remove(tabName);
				} else {
					event.doit = false;
				}
			}
		});
		
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
		experimentControlPanelComposite.setLayout(new GridLayout(5, false));
		
		Label experimentNameLabel = new Label(experimentControlPanelComposite, SWT.NONE);
		experimentNameLabel.setSize(66, 14);
		experimentNameLabel.setText("Experiment Label: ");
		
		final Text experimentLabelText = new Text(experimentControlPanelComposite, SWT.BORDER);
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
		
		// set the tab selection to this newly created tab
		experimentFolder.setSelection(experimentFolder.getItemCount()-1);
	}

	@Override
	public void setFocus() {
		// intentionally left blank
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
	
	private static String getUniqueName(String experimentName){
		int suffix = 2;
		while(experiments.containsKey(experimentName)){
			experimentName = experimentName + " " + (suffix++);
		}
		return experimentName;
	}
	
}
