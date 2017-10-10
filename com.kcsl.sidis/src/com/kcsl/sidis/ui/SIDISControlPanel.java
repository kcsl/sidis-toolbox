package com.kcsl.sidis.ui;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;

public class SIDISControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.controlpanel"; //$NON-NLS-1$

	public SIDISControlPanel() {
		setPartName("SIDIS Control Panel");
		setTitleImage(ResourceManager.getPluginImage("com.kcsl.sidis", "icons/toolbox.gif"));
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		SashForm sashForm = new SashForm(parent, SWT.NONE);
		
		Composite composite = new Composite(sashForm, SWT.NONE);
		
		Composite composite_1 = new Composite(sashForm, SWT.NONE);
		sashForm.setWeights(new int[] {1, 1});
		
	}

	@Override
	public void setFocus() {}
}
