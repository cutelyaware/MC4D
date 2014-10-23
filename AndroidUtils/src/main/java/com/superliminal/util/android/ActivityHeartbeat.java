package com.superliminal.util.android;

import android.app.Activity;

/**
 * Meant for code that must run at some frequency while an Activity is running.
 * Call onPause and onResume methods in your corresponding activity methods.
 * 
 * @author Melinda Green
 */
public class ActivityHeartbeat {
    private Activity activity;
    private Thread heart = null;
    private Runnable beat;
    private long frequencyMilliseconds;

    /**
     * The given code will be run at the given frequency while the given Activity is active.
     * It is always called on the UI thread.
     * 
     * @param activity Activity associated with this heartbeat.
     * @param frequencyMilliseconds How often to beat.
     * @param onBeat Caller's code to be executed once per heartbeat.
     */
    public ActivityHeartbeat(Activity activity, int frequencyMilliseconds, Runnable onBeat) {
        this.activity = activity;
        this.frequencyMilliseconds = frequencyMilliseconds;
        this.beat = onBeat;
    }

    /**
     * Always call this method for each of your ActivityHeartbeat objects
     * each time your onPause method is invoked. This stops all beats for this
     * ActivityHeartbeat until onResume restarts it.
     */
    public void onPause() {
        heart = null; // Gracefully stops the thread.
    }

    /**
     * Always call this method for each of your ActivityHeartbeat objects
     * each time your onResume method is invoked. This restarts this
     * ActivityHeartbeat until onPause stops it.
     */
    public void onResume() {
        heart = new Thread("Heartbeat") {
            @Override
            public void run() {
                while(heart != null) {
                    activity.runOnUiThread(beat);
                    try {
                        sleep(frequencyMilliseconds);
                    } catch(InterruptedException e) {}
                }
            }
        };
        heart.setPriority(Thread.MIN_PRIORITY);
        heart.start();
    }

}
