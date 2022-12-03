package com.superliminal.util.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

//From http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html

public class GUITaskQueue {
  private static final int HANDLE_EXCEPTION = 0x1337;
  private static final int HANDLE_AFTER_EXECUTE = 0x1338;
  private TaskQueue taskQ;
  private Handler handler;
  private static GUITaskQueue singleton;

  public static GUITaskQueue getInstance() {
    if (singleton == null) {
      singleton = new GUITaskQueue();
      singleton.start();
    }
    return singleton;
  }

  private GUITaskQueue() {
    taskQ = new TaskQueue();
    handler = new MyHandler();
  }

  public void start() {
    taskQ.start();
  }

  public void stop() {
    taskQ.stop();
  }

  public void addTask(GUITask task) {
    taskQ.addTask(new GUITaskAdapter(task));
  }

  /**
   * Adds a task with an associated progress indicator. The indicator's showProgressBar() gets
   * called immediately then the hideProgressBar() gets called before the GUITask's
   * handle_exception() or after_execute() method gets called.
   */
  public void addTask(ProgressBar progressIndicator, GUITask task) {
    if (progressIndicator == null) {
      addTask(task);
    } else {
      addTask(new GUITaskWithProgress(task, progressIndicator));
    }
  }

  private static class GUITaskWithProgress implements GUITask {
    private GUITask mTask;
    private ProgressBar progressIndicator;

    GUITaskWithProgress(GUITask task, ProgressBar _progressIndicator) {
      mTask = task;
      progressIndicator = _progressIndicator;
      progressIndicator.setVisibility(View.VISIBLE);
    }

    public void executeNonGuiTask() throws Exception {
      mTask.executeNonGuiTask();
    }

    public void onFailure(Throwable t) {
      progressIndicator.setVisibility(View.INVISIBLE);
      mTask.onFailure(t);
    }

    public void after_execute() {
      progressIndicator.setVisibility(View.INVISIBLE);
      mTask.after_execute();
    }
  };


  private static class GUITaskWithSomething<T> {
    GUITask guiTask;
    T something;

    GUITaskWithSomething(GUITask _guiTask, T _something) {
      guiTask = _guiTask;
      something = _something;
    }
  }

  private void postMessage(int what, Object thingToPost) {
    Message msg = new Message();
    msg.obj = thingToPost;
    msg.what = what;
    handler.sendMessage(msg);
  }

  private void postException(GUITask task, Throwable t) {
    postMessage(HANDLE_EXCEPTION, new GUITaskWithSomething<Throwable>(task, t));
  }

  private static class MyHandler extends Handler {
    MyHandler() {
      super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case HANDLE_EXCEPTION:
          @SuppressWarnings("unchecked")
          GUITaskWithSomething<Throwable> thingie = (GUITaskWithSomething<Throwable>) msg.obj;
          thingie.guiTask.onFailure(thingie.something);
          break;

        case HANDLE_AFTER_EXECUTE:
          GUITask task = (GUITask) msg.obj;
          try {
            task.after_execute();
          } catch (Throwable t) {
            //LogX.e(t);
          }
          break;
      }
      super.handleMessage(msg);
    }
  }

  private class GUITaskAdapter implements Runnable {
    private GUITask task;

    GUITaskAdapter(GUITask _task) {
      task = _task;
    }

    public void run() {
      try {
        task.executeNonGuiTask();
        postMessage(HANDLE_AFTER_EXECUTE, task);
      } catch (Throwable t) {
        postException(task, t);
      }
    }
  }
}

