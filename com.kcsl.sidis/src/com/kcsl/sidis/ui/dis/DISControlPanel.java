package com.kcsl.sidis.ui.dis;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;

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
		
	}

	@Override
	public void setFocus() {}
}
