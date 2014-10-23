package com.superliminal.util.android;

public class ChartUtils {
	public static class ChartSteps {
		public int stepCount = 1;
		public int stepSize = 1; // start at $1. stocks trading for less than that not handled yet
	}
	
	/**
	 * Calculates a step size in dollars that generates a small but not too small
	 * number of price lines.
	 */
	public static ChartSteps calcSteps(float high_price) {
		ChartSteps step_data = new ChartSteps();
		while(high_price / step_data.stepSize > 12)  // too many lines
			step_data.stepSize *= 10;
		step_data.stepCount = (int)high_price / step_data.stepSize;
		if(step_data.stepCount * step_data.stepSize < high_price) // graph peak below step boundary?
			step_data.stepCount++; // need one more step to show top fraction 
		return step_data;
	}
}
