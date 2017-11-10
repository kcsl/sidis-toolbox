package com.kcsl.sidis.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.kcsl.sidis.Activator;
import com.kcsl.sidis.log.Log;

public class SIDISPreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;

	/**
	 * Enable/disable general logging
	 */
	public static final String GENERAL_LOGGING = "GENERAL_LOGGING";
	public static final Boolean GENERAL_LOGGING_DEFAULT = true;
	private static boolean generalLoggingValue = GENERAL_LOGGING_DEFAULT;
	
	/**
	 * Configures general logging
	 */
	public static void enableGeneralLogging(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(GENERAL_LOGGING, enabled);
		loadPreferences();
	}
	
	public static boolean isGeneralLoggingEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return generalLoggingValue;
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setDefault(GENERAL_LOGGING, GENERAL_LOGGING_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(GENERAL_LOGGING, GENERAL_LOGGING_DEFAULT);
		loadPreferences();
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			generalLoggingValue = preferences.getBoolean(GENERAL_LOGGING);
		} catch (Exception e){
			Log.warning("Error accessing SIDIS preferences, using defaults...", e);
		}
		initialized = true;
	}
}