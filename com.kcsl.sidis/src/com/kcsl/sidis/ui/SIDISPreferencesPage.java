package com.kcsl.sidis.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ensoftcorp.open.commons.ui.components.LabelFieldEditor;
import com.kcsl.sidis.Activator;
import com.kcsl.sidis.preferences.SIDISPreferences;

/**
 * UI for setting SIDIS preferences
 * 
 * @author Ben Holland
 */
public class SIDISPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	private static final String GENERAL_LOGGING_DESCRIPTION = "General Logging";
	
	private static boolean changeListenerAdded = false;
	
	public SIDISPreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(preferences);
		setDescription("Configure preferences for the SIDIS toolbox plugin.");
		
		// use to update cached values if user edits a preference
		if(!changeListenerAdded){
			getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
					SIDISPreferences.loadPreferences();
				}
			});
			changeListenerAdded = true;
		}
	}

	@Override
	protected void createFieldEditors() {
		addField(new LabelFieldEditor("Dynamically-Informed Static (DIS) Analysis Options", getFieldEditorParent()));
		addField(new BooleanFieldEditor(SIDISPreferences.GENERAL_LOGGING, "&" + GENERAL_LOGGING_DESCRIPTION, getFieldEditorParent()));
	}

}
