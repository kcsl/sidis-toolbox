package com.kcsl.sidis.ui.dis;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;

import com.ensoftcorp.open.commons.utilities.DisplayUtils;
import com.kcsl.sidis.dis.Import;

public class DISControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.dis.controlpanel"; //$NON-NLS-1$

	public DISControlPanel() {
		setPartName("DIS Control Panel");
		setTitleImage(ResourceManager.getPluginImage("com.kcsl.sidis", "icons/toolbox.gif"));
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		Group grpImportStatementExecution = new Group(parent, SWT.NONE);
		grpImportStatementExecution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpImportStatementExecution.setLayout(new GridLayout(2, false));
		grpImportStatementExecution.setText("Import Statement Execution Counts (sidis.ec.dat)");
		
		Button browseECButton = new Button(grpImportStatementExecution, SWT.NONE);
		browseECButton.setText("Browse...");

		Button purgePreviousECResultsButton = new Button(grpImportStatementExecution, SWT.CHECK);
		purgePreviousECResultsButton.setSelection(true);
		purgePreviousECResultsButton.setText("Purge Previous Results");
		
		browseECButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialogEC = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
			    fileDialogEC.setFilterNames(new String[] { "Execution Count Data Files", "All Files (*.*)" });
			    fileDialogEC.setFilterExtensions(new String[] { "*.ec.dat", "*.*" });
			    String path = fileDialogEC.open();
			    final boolean purgePrevious = purgePreviousECResultsButton.getSelection();
			    if(path != null){
			    	File inputECFile = new File(path);
			    	if(inputECFile.exists()){
			    		Job job = new Job("Import Statement Execution Counts") {
			    		    @Override
			    		    protected IStatus run(IProgressMonitor monitor) {
			    		    	try {
			    		    		Import.loadStatementExecutionCountData(inputECFile, purgePrevious);
					            } 
			    		    	// TODO: implement a cancel operation
//			    		    	catch (InterruptedException e) {
//					                return Status.CANCEL_STATUS;
//					            } 
			    		    	catch (FileNotFoundException e) {
			    		    		DisplayUtils.showError(e, "File not found!");
					            	return Status.CANCEL_STATUS;
								}
			    		        return Status.OK_STATUS;
			    		    }

			    		};
			    		job.schedule();
			    	}
			    }
			}
		});
	}

	@Override
	public void setFocus() {}
}
