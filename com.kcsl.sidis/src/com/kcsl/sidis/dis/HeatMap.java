package com.kcsl.sidis.dis;

import java.awt.Color;

public class HeatMap {

	/**
	 * Returns a heat map color for the given intensity (range 0.0 to 1.0)
	 * Black represents cold (0.0 intensity) and White represents hot (1.0 intensity)
	 * @param intensity
	 * @return
	 */
	public static Color getMonochromeHeatMapColor(double intensity) {
		if(intensity < 0.0 || intensity > 1.0){
			throw new IllegalArgumentException("Invalid intensity value");
		}
		int r = (int) Math.round(intensity * 255.0);
		int g = (int) Math.round(intensity * 255.0);
		int b = (int) Math.round(intensity * 255.0);
		return new Color(r, g, b);
	}
	
	/**
	 * Returns a heat map color for the given intensity (range 0.0 to 1.0)
	 * White represents cold (0.0 intensity) and Black represents hot (1.0 intensity)
	 * @param intensity
	 * @return
	 */
	public static Color getInvertedMonochromeHeatMapColor(double intensity) {
		if(intensity < 0.0 || intensity > 1.0){
			throw new IllegalArgumentException("Invalid intensity value");
		}
		int r = (int) Math.round(255.0 - (intensity * 255.0));
		int g = (int) Math.round(255.0 - (intensity * 255.0));
		int b = (int) Math.round(255.0 - (intensity * 255.0));
		return new Color(r, g, b);
	}
	
	/**
	 * Returns a heat map color for the given intensity (range 0.0 to 1.0)
	 * Blue represents cold (0.0 intensity) and Red represents hot (1.0 intensity)
	 * @param intensity
	 * @return
	 */
	public static Color getBlueRedGradientHeatMapColor(double intensity) {
		if(intensity < 0.0 || intensity > 1.0){
			throw new IllegalArgumentException("Invalid intensity value");
		}
		Color cold = Color.BLUE.darker();
		Color hot = Color.RED.darker();
		return get2ColorGradientHeatMapColor(intensity, cold, hot);
	}
	
	/**
	 * Returns a heat map color for the given intensity (range 0.0 to 1.0)
	 * @param intensity
	 * @param cold The color to represent intensity 0.0
	 * @param hot The color to represent intensity 1.0
	 * @return
	 */
	public static Color get2ColorGradientHeatMapColor(double intensity, Color cold, Color hot) {
		if(intensity < 0.0 || intensity > 1.0){
			throw new IllegalArgumentException("Invalid intensity value");
		}
		int r = (int) Math.round((hot.getRed() - cold.getRed()) * intensity + cold.getRed());
		int g = (int) Math.round((hot.getGreen() - cold.getGreen()) * intensity + cold.getGreen());
		int b = (int) Math.round((hot.getBlue() - cold.getBlue()) * intensity + cold.getBlue());
		return new Color(r,g,b);
	}
	
	/**
	 * Given the lower bound, upper bound, and actual value, normalize the
	 * actual value to a value between 0.0 and 1.0
	 * 
	 * @param value
	 * @param minValue
	 * @param maxValue
	 * @return
	 */
	public static double normalizeIntensity(long value, long minValue, long maxValue){
		double range = (double) maxValue - (double) minValue;
		return ((double) value - (double) minValue) / range;
	}

	/**
	 * Given the lower bound, upper bound, and actual value, normalize the
	 * actual value to a log base 10 value between 0.0 and 1.0
	 * @param value
	 * @param minValue
	 * @param maxValue
	 * @return
	 */
	public static double normalizeLogarithmicIntensity(Long value, long minValue, long maxValue) {
		double scaledValue = value == 0 ? 0.0 : Math.log10(value);
		double scaledMinValue = minValue == 0 ? 0.0 : Math.log10(minValue);
		double scaledMaxValue = maxValue == 0 ? 0.0 : Math.log10(maxValue);
		double scaledRange = scaledMaxValue - scaledMinValue;
		return (scaledValue - scaledMinValue) / scaledRange;
	}
	
}
