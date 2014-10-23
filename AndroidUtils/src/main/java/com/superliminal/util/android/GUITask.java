package com.superliminal.util.android;

// From http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html

public interface GUITask {
	void executeNonGuiTask() throws Exception;
	void after_execute();
	void onFailure(Throwable t);
}
