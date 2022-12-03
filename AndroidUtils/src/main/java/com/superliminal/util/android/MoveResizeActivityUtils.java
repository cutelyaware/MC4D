package com.superliminal.util.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

/**
 * Testing utility methods that allow the user to resize Activities
 * to see how given Activities will appear at arbitrary resolutions.
 * 
 * @author Don Hatch
 */
public class MoveResizeActivityUtils {
    private final static boolean DEBUG = false;
    private final static String TAG = "MoveResizeActivityUtils";
    private static void LOG(String text) {
        Log.v(TAG, text);
    }
    private final static int DEFAULT_HANDLE_SIZE = 60;

    // Unique layout class just so the unwrapping code can be sure Activity was previously wrapped.
    private static class Wrapper extends RelativeLayout {
        public Wrapper(Context context) {
            super(context);
        }
    };

    /**
     * Takes an already inflated Activity and wraps it's content in a resizable view.
     */
    public static void addResizeHandle(Activity act, int handleWidth, int handleHeight) {
        View root = ((ViewGroup) act.findViewById(android.R.id.content)).getChildAt(0);
        ((ViewGroup) root.getParent()).removeView(root);
        act.setContentView(MoveResizeActivityUtils.addResizeHandle(root, handleWidth, handleHeight));
    }

    public static void addResizeHandle(Activity act) {
        addResizeHandle(act, DEFAULT_HANDLE_SIZE, DEFAULT_HANDLE_SIZE);
    }

    /**
     * Unwraps a previously resizable Activity.
     */
    public static void removeResizeHandle(Activity act) {
        View root = ((ViewGroup) act.findViewById(android.R.id.content)).getChildAt(0);
        if(!(root instanceof Wrapper)) {
            throw new IllegalStateException("Root not a resizable wrapper");
        }
        Wrapper wrapper = (Wrapper) root;
        View originalRoot = wrapper.getChildAt(0);
        wrapper.removeViewAt(0);
        act.setContentView(originalRoot);
    }

    /**
     * Wraps the child in a layout with a little resize handle at the lower right,
     * and returns the layout.
     * assumes child's context is an Activity.
     * 
     * NOTE: currently this is implemented using a RelativeLayout,
     * which interferes with layout performance and timing.
     * Specifically, RelativeLayout is one of the layouts
     * whose onMeasure() measures its children twice,
     * contributing to the overall exponential behavior.
     * It should be rewritten using another (possibly custom)
     * viewgroup without this intrusive behavior.
     */
    public static View addResizeHandle(final View child, final int handleWidth, final int handleHeight) {
        final Context context = child.getContext();
        final Activity activity = (Activity) context;

        final View.OnTouchListener handleTouchListener = new View.OnTouchListener() {
            private boolean fingerIsDown = false;
            private int fingerDownWindowRect[] = new int[4];
            private float fingerDownX = 0.f;
            private float fingerDownY = 0.f;
            private float prevFingerX = 0.f;
            private float prevFingerY = 0.f;

            public boolean onTouch(View v, MotionEvent event) {
                if(DEBUG)
                    LOG("in button onTouch");
                int eventAction = event.getAction();
                // use getRawX/Y for screen coords
                // (rather than getX/Y for window coords, which is a moving target)
                float fingerX = event.getRawX();
                float fingerY = event.getRawY();
                if(DEBUG)
                    LOG("    action = " + motionEventActionToString(eventAction));
                if(DEBUG)
                    LOG("    x,y = " + fingerX + " " + fingerY);
                Window window = activity.getWindow();
                if(DEBUG)
                    LOG("    window = " + window);
                android.view.WindowManager.LayoutParams layoutParams = window.getAttributes();
                if(DEBUG)
                    LOG("    layoutParams = " + layoutParams);
                if(fingerIsDown
                    && (fingerX != prevFingerX
                    || fingerY != prevFingerY))
                {
                    int origX0 = fingerDownWindowRect[0];
                    int origY0 = fingerDownWindowRect[1];
                    int origX1 = fingerDownWindowRect[2];
                    int origY1 = fingerDownWindowRect[3];
                    int newX0 = origX0;
                    int newY0 = origY0;
                    int newX1 = (int) (origX1 + (fingerX - fingerDownX));
                    int newY1 = (int) (origY1 + (fingerY - fingerDownY));
                    setWindowRectOnScreen(window, newX0, newY0, newX1, newY1);
                }
                if(eventAction == MotionEvent.ACTION_DOWN) {
                    fingerIsDown = true;
                    fingerDownX = fingerX;
                    fingerDownY = fingerY;
                    getWindowRectOnScreen(window, fingerDownWindowRect);
                    if(DEBUG)
                        LOG("            on down event, rect was [" + fingerDownWindowRect[0] + ".." + fingerDownWindowRect[2] + "]x[" + fingerDownWindowRect[1] + ".." + fingerDownWindowRect[3] + "] = " + (fingerDownWindowRect[2] - fingerDownWindowRect[0]) + "x" + (fingerDownWindowRect[3] - fingerDownWindowRect[1]));
                }
                else if(eventAction == MotionEvent.ACTION_UP) {
                    fingerIsDown = false;
                }
                prevFingerX = fingerX;
                prevFingerY = fingerY;
                if(DEBUG)
                    LOG("out button onTouch");
                return false; // don't consume (so buttons change colors normally)
            }
        }; // handleTouchListener

        return new Wrapper(context) {
            @Override
            public void onMeasure(int wSpec, int hSpec) {
                if(DEBUG)
                    LOG("in moveresizeActivityUtils relativeLayout onMeasure");
                super.onMeasure(wSpec, hSpec);
                if(DEBUG)
                    LOG("out moveresizeActivityUtils relativeLayout onMeasure");
            }
            {
                addView(child,
                    new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
                addView(new Button(context) {
                    @Override
                    protected void onDraw(Canvas canvas) {
                        if(DEBUG)
                            LOG("in button onDraw");
                        // There's no need to call super.onDraw(canvas) any more
                        // since the Button parts are now invisible...
                        // in fact we need not subclass from Button at all.
                        // TODO: it would still be nice to make it look
                        // visibly different when finger is down, like Button does, though.
                        Path path = new Path() {
                            {
                                moveTo(handleWidth - 1, handleHeight - 1);
                                lineTo(0, handleHeight - 1);
                                lineTo(handleWidth - 1, 0);
                                close();
                            }
                        };
                        Paint paint = new Paint();
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.WHITE);
                        paint.setAlpha(255 * 3 / 4);
                        canvas.drawPath(path, paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(1.f);
                        paint.setAntiAlias(true);
                        paint.setColor(Color.BLUE);
                        canvas.drawPath(path, paint);
                        if(DEBUG)
                            LOG("out button onDraw");
                    } // end onDraw()
                    {
                        setBackgroundColor(0x000000);
                        setOnTouchListener(handleTouchListener);
                    }
                },
                    new RelativeLayout.LayoutParams(handleWidth, handleHeight) {
                        {
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        }
                    });
            }
        };
    } // addResizeHandle

    /**
     * @returns {x0,y0,x1,y1}
     */
    private static void getWindowRectOnScreen(Window window, int rect[/* 4 */]) {
        View decorView = window.getDecorView();
        decorView.getLocationOnScreen(rect); // into rect[0], rect[1]
        rect[2] = rect[0] + decorView.getMeasuredWidth(); // x1 = x0 + width
        rect[3] = rect[1] + decorView.getMeasuredHeight(); // y1 = y0 + height
    }
    private static void setWindowRectOnScreen(Window window, int x0, int y0, int x1, int y1) {
        if(DEBUG)
            LOG("            setting window rect to [" + x0 + ".." + x1 + "]x[" + y0 + ".." + y1 + "] = " + (x1 - x0) + "x" + (y1 - y0));
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        // If we leave gravity as NO_GRAVITY
        // then that means center (apparently)
        // which makes the relationship between layoutParams.x,y
        // and the final screen x,y a mess,
        // and difficult-to-impossible to predict exactly.
        // So specify top-left gravity instead,
        // which allows us to specify the upper-left corner exactly as x,y.
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = x0;
        layoutParams.y = y0;
        layoutParams.width = x1 - x0;
        layoutParams.height = y1 - y0;
        window.setAttributes(layoutParams);
    }

    // MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL" etc.
    private static String motionEventActionToString(int action) {
        java.lang.reflect.Field fields[] = MotionEvent.class.getDeclaredFields();
        for(int iField = 0; iField < fields.length; ++iField) {
            java.lang.reflect.Field field = fields[iField];
            if(field.getType() == int.class
                && field.getName().startsWith("ACTION_")) {
                try {
                    if(field.getInt(null) == action)
                        return field.getName();
                } catch(IllegalAccessException e) {}
            }
        }
        return "(action " + action + " ???)";
    } // end motionEventActionToString()

} // end class MoveResizeActivityUtils
