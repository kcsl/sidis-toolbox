package com.kcsl.sidis.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ensoftcorp.open.commons.ui.components.LabelFieldEditor;
import com.ensoftcorp.open.commons.ui.components.SpacerFieldEditor;

import com.kcsl.sidis.Activator;
import com.kcsl.sidis.preferences.SIDISPreferences;

/**
 * UI for setting SIDIS preferences
 * 
 * @author Ben Holland
 */
public class SIDISPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String MONOCHROME_COLOR_GRADIENT_DESCRIPTION = "Monochrome Color Gradient (Cold: Black, Hot: White)";
	private static final String INVERTED_MONOCHROME_COLOR_GRADIENT_DESCRIPTION = "Inverted Monochrome Color Gradient (Cold: White, Hot: Black)";
	private static final String BLUE_RED_COLOR_GRADIENT_DESCRIPTION = "2-Color Blue/Red Color Gradient (Cold: Blue, Hot: Red)";
	
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
		RadioGroupFieldEditor analysisMode = new RadioGroupFieldEditor(
				SIDISPreferences.PREFERRED_HEAT_MAP_COLOR_SCHEME,
				"DIS Control Flow Heat Map Overlay Color Scheme",
				1,
				new String[][] {
					{ "&" + MONOCHROME_COLOR_GRADIENT_DESCRIPTION, 
						SIDISPreferences.MONOCHROME_COLOR_GRADIENT
					},
					{ "&" + INVERTED_MONOCHROME_COLOR_GRADIENT_DESCRIPTION, 
						SIDISPreferences.INVERTED_MONOCHROME_COLOR_GRADIENT
					},
					{ "&" + BLUE_RED_COLOR_GRADIENT_DESCRIPTION, 
						SIDISPreferences.BLUE_RED_COLOR_GRADIENT
					}
				},
				getFieldEditorParent(),
				true);
		addField(analysisMode);
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("General Options", getFieldEditorParent()));
		addField(new BooleanFieldEditor(SIDISPreferences.GENERAL_LOGGING, "&" + GENERAL_LOGGING_DESCRIPTION, getFieldEditorParent()));
	}

}
