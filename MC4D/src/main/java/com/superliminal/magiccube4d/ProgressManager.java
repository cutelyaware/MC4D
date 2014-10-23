package com.superliminal.magiccube4d;

/*
 * Subclass of SwingWorker that manages a JProgressBar and includes
 * initialization and update methods that affect it.
 * Initialization will turn the progress bar to be visible,
 * and invisible when done.
 * Initialization can put the progress bar into determinant or indeterminant mode.
 * When in determinant mode, use the updateProgress method to set the bar position
 * between 0 and the supplied max values.
 * 
 * @author Melinda Green
 */
public class ProgressManager {
	private ProgressView progressView;
	private int max;
	
	public ProgressManager(ProgressView progress) {
		this.progressView = progress;
//		this.addPropertyChangeListener(
//  		     new PropertyChangeListener() {
//   		    	 @Override
//   		         public  void propertyChange(PropertyChangeEvent evt) {
//   		    		 String eventName = evt.getPropertyName();
//   		             if ("progress".equals(eventName)) {
//   			        	 final int newVal = ((Integer)evt.getNewValue()).intValue();
//   		                 progressView.setValue(newVal);
//   		                 progressView.repaint();
//   		             }
//   		         }
//   		     }
//   	    );
	}
	
	private void init(final String string, final boolean indeterminate, int max) {
		this.max = max;
		setProgress(0);
		progressView.setString(string);
	}
	
	private void setProgress(int i) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * Initializes the progress bar in determinate mode.
	 */
	public void init(String string, int max) {
		init(string, false, max);
	}

	/*
	 * Initializes the progress bar in indeterminate mode.
	 */
	public void init(String string) {
		init(string, true, 1);
	}
	
	public void updateProgress(int progress) {
		int prog = (int)(100.0 * progress/max);
		//System.out.println(progressView.getString() + " " + progress + " out of " + max);
		progressView.setProgress(prog);
	}
	
	public void done() {
		progressView.setVisible(false);
	}
}

