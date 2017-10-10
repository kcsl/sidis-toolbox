package com.kcsl.sidis.ui.sid;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;

public class SIDControlPanel extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.sid.controlpanel"; //$NON-NLS-1$

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
		
	}

	@Override
	public void setFocus() {}
}
