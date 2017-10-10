package com.kcsl.sidis.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.kcsl.sidis.Activator;
import com.kcsl.sidis.log.Log;

public class SIDISPreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;
	
	public static final String PREFERRED_HEAT_MAP_COLOR_SCHEME = "PREFERRED_HEAT_MAP_COLOR_SCHEME";
	public static final String MONOCHROME_COLOR_GRADIENT = "MONOCHROME_COLOR_GRADIENT";
	public static final String INVERTED_MONOCHROME_COLOR_GRADIENT = "INVERTED_MONOCHROME_COLOR_GRADIENT";
	public static final String BLUE_RED_COLOR_GRADIENT = "BLUE_RED_COLOR_GRADIENT";
	public static final String PREFERRED_HEAT_MAP_COLOR_SCHEME_DEFAULT = BLUE_RED_COLOR_GRADIENT;
	private static String preferredHeatMapColorSchemeValue = PREFERRED_HEAT_MAP_COLOR_SCHEME_DEFAULT;
	
	/**
	 * Configures the preferred heat map color scheme to use a monochrome (cold:
	 * black, hot: white) color scheme
	 */
	public static void setMonochromeColorGradient(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(PREFERRED_HEAT_MAP_COLOR_SCHEME, MONOCHROME_COLOR_GRADIENT);
		loadPreferences();
	}
	
	/**
	 * Returns true if the monochrome color gradient color scheme is enabled
	 * @return
	 */
	public static boolean isMonochromeColorGradiantEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return preferredHeatMapColorSchemeValue.equals(MONOCHROME_COLOR_GRADIENT);
	}
	
	/**
	 * Configures the preferred heat map color scheme to use an inverted monochrome (cold: white
	 * black, hot: black) color scheme
	 */
	public static void setInvertedMonochromeColorGradient(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(PREFERRED_HEAT_MAP_COLOR_SCHEME, INVERTED_MONOCHROME_COLOR_GRADIENT);
		loadPreferences();
	}
	
	/**
	 * Returns true if the inverted monochrome color gradient color scheme is enabled
	 * @return
	 */
	public static boolean isInvertedMonochromeColorGradiantEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return preferredHeatMapColorSchemeValue.equals(INVERTED_MONOCHROME_COLOR_GRADIENT);
	}
	
	/**
	 * Configures the preferred heat map color scheme to use an two color gradient (cold: blue
	 * black, hot: red) color scheme
	 */
	public static void setBlueRedColorGradient(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(PREFERRED_HEAT_MAP_COLOR_SCHEME, BLUE_RED_COLOR_GRADIENT);
		loadPreferences();
	}
	
	/**
	 * Returns true if the inverted monochrome color gradient color scheme is enabled
	 * @return
	 */
	public static boolean isBlueRedColorGradiantEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return preferredHeatMapColorSchemeValue.equals(BLUE_RED_COLOR_GRADIENT);
	}
	
	/**
	 * Enable/disable logarithmic scale heat map
	 */
	public static final String LOGARITHMIC_SCALE_HEAT_MAP = "LOGARITHMIC_SCALE_HEAT_MAP";
	public static final Boolean LOGARITHMIC_SCALE_HEAT_MAP_DEFAULT = false;
	private static boolean logarithmicHeatMapValue = LOGARITHMIC_SCALE_HEAT_MAP_DEFAULT;
	
	/**
	 * Configures logarithmic scale heat map
	 */
	public static void enableLogarithmicScaleHeatMap(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(LOGARITHMIC_SCALE_HEAT_MAP, enabled);
		loadPreferences();
	}
	
	/**
	 * Returns true if logarithmic scale heat map is enabled
	 * @return
	 */
	public static boolean isLogarithmicScaleHeatMapEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return logarithmicHeatMapValue;
	}
	
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
		preferences.setDefault(PREFERRED_HEAT_MAP_COLOR_SCHEME, PREFERRED_HEAT_MAP_COLOR_SCHEME_DEFAULT);
		preferences.setDefault(LOGARITHMIC_SCALE_HEAT_MAP, LOGARITHMIC_SCALE_HEAT_MAP_DEFAULT);
		preferences.setDefault(GENERAL_LOGGING, GENERAL_LOGGING_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(PREFERRED_HEAT_MAP_COLOR_SCHEME, PREFERRED_HEAT_MAP_COLOR_SCHEME_DEFAULT);
		preferences.setValue(LOGARITHMIC_SCALE_HEAT_MAP, LOGARITHMIC_SCALE_HEAT_MAP_DEFAULT);
		preferences.setValue(GENERAL_LOGGING, GENERAL_LOGGING_DEFAULT);
		loadPreferences();
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			preferredHeatMapColorSchemeValue = preferences.getString(PREFERRED_HEAT_MAP_COLOR_SCHEME);
			logarithmicHeatMapValue = preferences.getBoolean(LOGARITHMIC_SCALE_HEAT_MAP);
			generalLoggingValue = preferences.getBoolean(GENERAL_LOGGING);
		} catch (Exception e){
			Log.warning("Error accessing SIDIS preferences, using defaults...", e);
		}
		initialized = true;
	}
}