package com.superliminal.magiccube4d;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.superliminal.magiccube4d.MagicCube.InputMode;
import com.superliminal.util.PropertyManager;
import com.superliminal.util.ResourceUtils;
import com.superliminal.util.android.Color;
import com.superliminal.util.android.Graphics;
import com.superliminal.util.android.Graphics.Font;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

public class MC4DAndroidView extends View {
    private boolean DEBUGGING = false;
    private final float PLANCHETTE_OFFSET_Y = 100;
    private final float PLANCHETTE_WIDTH = 50;
    private final float PLANCHETTE_HEIGHT = 70;
    private int stickerUnderPlanchette = -1;

    private PuzzleManager puzzleManager;
    private RotationHandler rotationHandler = new RotationHandler(MagicCube.NICE_VIEW);
    public RotationHandler getRotations() { return rotationHandler; }
    private float[] lastDrag0, lastDrag1; // Most recent position of fingers. Non-null == dragging.
    private float[] lastDragSave = new float[2]; // never null
    private float[] lastStart0, lastStart1; // Positions at initial pointer down.
    private long lastDown0, lastDown1;
    private double pinchLastDist, pinchSF = 1;
    private int xOff, yOff;
    private float polys2pixelsSF = .01f; // screen transform data
    private int mY = 100;
    private AnimationQueue animationQueue;

    public MC4DAndroidView(final Context context, PuzzleManager puzMan, History hist) {
        super(context);
        puzzleManager = puzMan;
        puzzleManager.addPuzzleListener(new PuzzleManager.PuzzleListener() {
            public void puzzleChanged(boolean newPuzzle) {
                // initMacroControls(); // to properly enable/disable the buttons
                // progressBar.setVisible(false);
                // hist.clear((int)puzzleManager.puzzleDescription.getEdgeLength());
                // updateTwistsLabel();
                Color[] userColors = findColors(puzzleManager.puzzleDescription.nFaces(), MagicCube.FACE_COLORS_FILE);
                if(userColors != null)
                    puzzleManager.faceColors = userColors;
                // if(view != null)
                // view.repaint();
            }
        });
        animationQueue = new AnimationQueue(hist);
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // dumpMotionEvent(event);
                int pid = event.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                long now = event.getEventTime();
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: // The first pointer down out of however many.
                        rotationHandler.stopSpinning();
                        mY = (int) event.getY();
                        float[] here = new float[]{event.getX(), (int) event.getY()};
                        if(event.getPointerId(0) == 0) {
                            lastDragSave = lastDrag0 = lastStart0 = here;
                            lastDown0 = now;
                        }
                    else { // must be the second finger. Does this case happen?
                        lastDrag1 = lastStart1 = here;
                        lastDown1 = now;
                    }
                    break;
                    case MotionEvent.ACTION_POINTER_DOWN: // This is the second or later pointer down.
                        float[]
                              pos0 = new float[]{event.getX(0), (int) event.getY(0)},
                              pos1 = new float[]{event.getX(1), (int) event.getY(1)};
                              if(event.getPointerId(pid) == 0) {
                                  lastDragSave = lastDrag0 = lastStart0 = pos0;
                                  lastDown0 = now;
                              }
                        else {
                            lastDrag1 = lastStart1 = pos1;
                            lastDown1 = now;
                        }
                        float[] diff = new float[2];
                        Vec_h._VMV2(diff, pos0, pos1);
                        pinchLastDist = Math.sqrt(Vec_h._NORMSQRD2(diff));
                        break;
                    case MotionEvent.ACTION_UP: // The last pointer up out of however many.
                        float timeDiff = event.getEventTime() - (pid == 0 ? lastDown0 : lastDown1);
                        float[] curPoint = new float[]{event.getX(), (int) event.getY()};
                        float[] movement = new float[2];
                        float[] whichStart = pid == 0 ? lastStart0 : lastStart1;
                        if(whichStart != null) // Avoid possible NPE from crash reports.
                            Vec_h._VMV2(movement, whichStart, curPoint);
                        double moveDist = Math.sqrt(Vec_h._NORMSQRD2(movement));
                        if(moveDist < getWidth() / 40)
                            rotationHandler.stopSpinning(); // Treat drags smaller than 1/40th the screen width as a tap.
                        if(timeDiff < 1000 && moveDist < 5) {
                            // For rotating-face-to-center via tapping face:
                             boolean canrot = puzzleManager.canRotateToCenter((int)curPoint[0], (int)curPoint[1], rotationHandler);
                             if(canrot) {
                                 puzzleManager.clearStickerHighlighting();
                                 puzzleManager.mouseClickedAction(event, rotationHandler, PropertyManager.getFloat("twistfactor", 1), -1, MC4DAndroidView.this);
                             }
    //                         // For twisting via tapping sticker:
    //                         int grip = PipelineUtils.pickGrip(
    //                         //curPoint[0], curPoint[1]
    //                         lastDragSave[0], lastDragSave[1],
    //                         puzzleManager.untwistedFrame,
    //                         puzzleManager.puzzleDescription);
    //                         // The twist might be illegal.
    //                         if(grip < 0)
    //                             System.out.println("missed");
    //                         else {
    //                             MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
    //                             clicked.id_within_puzzle = grip; // slamming new id. do we need to set the other members?
    //                             clicked.face = puzzleManager.puzzleDescription.getGrip2Face()[grip];
    //                             // System.out.println("face: " + clicked.face);
    //                             int dir = timeDiff < 250 ? -1 : 1; // (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;
    //                             animationQueue.append(new MagicCube.TwistData(clicked, dir, 1), true, false);
    //                         }
                        }
                        if(pid == 0)
                            lastDrag0 = null;
                        else
                            lastDrag1 = null;
                        break;
                    case MotionEvent.ACTION_POINTER_UP: // This is *not* the last pointer up.
                        if(pid == 0)
                            lastDrag0 = null;
                        else
                            lastDrag1 = null;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mY = (int) event.getY();
                        float[] end0 = new float[]{event.getX(0), event.getY(0)};
                        if(event.getPointerCount() == 1) { // Only rotate while a single finger is down.
                            float[]
                                drag_dir = new float[2],
                                last_pos = event.getPointerId(0) == 0 ? lastDrag0 : lastDrag1;
                                if(last_pos == null)
                                    return true; // This should not happen but it showed in a user's stack trace so test for it.
                                Vec_h._VMV2(drag_dir, last_pos, end0);
                                drag_dir[1] *= -1; // in Windows, Y is down, so invert it
                                // Pick our grip.
                                if(inputMode == InputMode.TWISTING)
                                    puzzleManager.updateStickerHighlighting(
                                            (int) event.getX(), (int) (event.getY() - PLANCHETTE_OFFSET_Y - PLANCHETTE_HEIGHT),
                                            1, false);
                                if(inputMode == InputMode.ROT_3D || inputMode == InputMode.ROT_4D)
                                    rotationHandler.mouseDragged(drag_dir[0], drag_dir[1], true, false, inputMode == InputMode.ROT_4D);
                            }
                            if(event.getPointerId(0) == 0) // Update the first pointer position. (There's always at least one.)
                                lastDragSave = lastDrag0 = end0;
                            else { // pointer ID at 0 must be 1 (or greater?)
                                lastDrag1 = end0;
                            }
                            if(event.getPointerCount() > 1) { // There's more than one (maybe assert count == 2?). Update that too.
                                float[]
                            end1 = new float[]{event.getX(1), event.getY(1)},
                            newdiff = new float[2];
                            Vec_h._VMV2(newdiff, end0, end1);
                            double pinchNewDist = Math.sqrt(Vec_h._NORMSQRD2(newdiff));
                            pinchSF *= pinchNewDist / pinchLastDist;
                            pinchSF = Math.max(pinchSF, 0.5);
                            pinchSF = Math.min(pinchSF, 5.0);
                            pinchLastDist = pinchNewDist;
                            lastDrag1 = end1;
                        }
                        break;
                    default:
                        return false;
                } // end switch()
                invalidate();
                return true;
            } // end OnTouch
        }); // OnTouchListener
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("onClick");
                Log.d("Touch", "onClick");
            }
        });
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(KeyEvent.isModifierKey(keyCode)) {
                    System.out.println("Modifier");
                    Log.d("Touch", "modifier");
                    return true;
                }
                return false;
            }
        });
    } // end MC4DAndroidView()

    /**
     * Show an event in the LogCat view, for debugging.
     * From http://www.zdnet.com/blog/burnette/how-to-use-multi-touch-in-android-2/1747
     **/
    private static void dumpMotionEvent(MotionEvent event) {
        String names[] = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if(actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(
                    action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for(int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if(i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
        String str = sb.toString();
        Log.d("Touch", str);
    }

    private int mFrameNum = 0;
    private long mLastFrameMillis = 0;
    private Font mDbgFont = new Font("Arial", Font.PLAIN, 10);

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        if(mFrameNum++ == 0)
            updateViewFactors();
        Graphics g = new Graphics(canvas);

        // Simple test+
        // int x = getWidth() / 2;
        // g.setColor(new Color(1.f, .0f, .0f));
        // int xs[] = {x, x-50, x+50 };
        // int ys[] = {mY-50, mY, mY };
        // g.fillPolygon(xs, ys, 3);

        // paint the puzzle
        if(puzzleManager != null && puzzleManager.puzzleDescription != null) {
            if(animationQueue.isAnimating() && puzzleManager.iTwist == puzzleManager.nTwist) {
                animationQueue.getAnimating();
                // time to stop the animation
                animationQueue.finishedTwist(); // end animation
                invalidate();
            }
            if(lastDrag0 == null && rotationHandler.continueSpin()) { // keep spinning
                invalidate();
            }

            final boolean do3DStepsOnly = false;
            PipelineUtils.AnimFrame frame = puzzleManager.computeFrame(
                    PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK),
                    PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK),
                    rotationHandler,
                    PropertyManager.getFloat("eyew", MagicCube.EYEW),
                    MagicCube.EYEZ * PropertyManager.getFloat("scale", 1),
                    polys2pixelsSF * (float) pinchSF,
                    xOff,
                    yOff,
                    MagicCube.SUNVEC,
                    PropertyManager.getBoolean("shadows", false), // false default for Android.
                    do3DStepsOnly,
                    this);
            puzzleManager.paintFrame(g,
                    frame,
                    PropertyManager.getBoolean("shadows", true),
                    PropertyManager.getBoolean("ground", true) ? PropertyManager.getColor("ground.color") : null,
                    PropertyManager.getBoolean("highlightbycubie", false),
                    PropertyManager.getBoolean("outlines", false) ? PropertyManager.getColor("outlines.color") : null,
                    PropertyManager.getFloat("twistfactor", 1));
        }

        if(inputMode == InputMode.TWISTING) {
            float x = lastDragSave[0];
            float y = lastDragSave[1];
            g.setColor(Color.green);
            g.drawTriangle(
                    x, y - PLANCHETTE_OFFSET_Y - PLANCHETTE_HEIGHT,
                    x - PLANCHETTE_WIDTH / 2, y - PLANCHETTE_OFFSET_Y,
                    x + PLANCHETTE_WIDTH / 2, y - PLANCHETTE_OFFSET_Y);
        }

        if(!DEBUGGING)
            return;
        g.setColor(Color.green);
        g.setFont(mDbgFont);
        long thisFrameMillis = System.currentTimeMillis();
        long fps = 1000 / (thisFrameMillis - mLastFrameMillis);
        g.drawString("FPS " + (fps < 10 ? "  " : "") + fps, 10, 60);
        mLastFrameMillis = thisFrameMillis;
    } // end onDraw (from MC4DView.paint())

    private void updateViewFactors() {
        int W = getWidth(), H = getHeight(), minpix = Math.min(W, H);
        if(minpix == 0)
            return;
        xOff = ((W > H) ? (W - H) / 2 : 0) + minpix / 2;
        yOff = ((H > W) ? (H - W) / 2 : 0) + minpix / 2;

        // Generate view-independent vertices for the current puzzle in its original 4D orientation, centered at the origin.
        final boolean do3DStepsOnly = true;
        PipelineUtils.AnimFrame frame = puzzleManager.computeFrame(
                PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK),
                PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK),
                this.rotationHandler,
                PropertyManager.getFloat("eyew", MagicCube.EYEW),
                MagicCube.EYEZ,
                1, // get coords in model coords
                0, 0, // No offset so that verts are centered.
                MagicCube.SUNVEC,
                false, // Don't let shadow polygons muck up the calculation.
                do3DStepsOnly,
                null);

        float radius3d = -1;
        int stickerInds[][][] = puzzleManager.puzzleDescription.getStickerInds();
        for(int i = 0; i < frame.drawListSize; i++) {
            int item[] = frame.drawList[i];
            int iSticker = item[0];
            int iPolyWithinSticker = item[1];
            int poly[] = stickerInds[iSticker][iPolyWithinSticker];
            for(int vertIndex : poly) {
                float dist = Vec_h._NORMSQRD3(frame.verts[vertIndex]);
                radius3d = Math.max(dist, radius3d);
            }
        }
        radius3d = (float) Math.sqrt(radius3d);
        // System.out.println("visible radius: " + radius3d);

        // This is what corrects the view scale for changes in puzzle and puzzle geometry.
        // To remove this correction, just set polys2pixelSF = minpix.
        polys2pixelsSF = minpix / radius3d;

        invalidate(); // Needed when a puzzle is read via Ctrl-O.
    } // end updateViewFactors

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return super.onTrackballEvent(event);
    }

    private InputMode inputMode = InputMode.TWISTING;

    public void setInputMode(InputMode mode) {
        inputMode = mode;
        if(inputMode == InputMode.TWISTING)
            puzzleManager.updateStickerHighlighting(
                    (int) lastDragSave[0],
                    (int) (lastDragSave[1] - PLANCHETTE_OFFSET_Y - PLANCHETTE_HEIGHT),
                    1, false);
        else
            puzzleManager.clearStickerHighlighting();
        invalidate();
    }

    public void twistSelected(int dir) {
        // int stickerUnderPlanchette = PipelineUtils.pickSticker(
        // lastDragSave[0], lastDragSave[1] - PLANCHETTE_OFFSET_Y - PLANCHETTE_HEIGHT,
        // puzzleManager.untwistedFrame,
        // puzzleManager.puzzleDescription);
        //
        int grip = PipelineUtils.pickGrip(
                // curPoint[0], curPoint[1]
                lastDragSave[0], lastDragSave[1] - PLANCHETTE_OFFSET_Y - PLANCHETTE_HEIGHT,
                puzzleManager.untwistedFrame,
                puzzleManager.puzzleDescription);
        // The twist might be illegal.
        if(grip < 0)
            System.out.println("missed");
        else {
            MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
            clicked.id_within_puzzle = grip; // slamming new id. do we need to set the other members?
            clicked.face = puzzleManager.puzzleDescription.getGrip2Face()[grip];
            // System.out.println("face: " + clicked.face);
            animationQueue.append(new MagicCube.TwistData(clicked, dir, 1), true, false);
            invalidate();
        }
    }

    private static Color[] findColors(int len, String fname) {
        for(Color[] cols : readColorLists(fname)) {
            if(cols.length == len)
                return cols;
        }
        return null;
    }

    private static Color[][] readColorLists(String fname) {
        URL furl = ResourceUtils.getResource(fname);
        if(furl == null)
            return new Color[0][];
        String contents = ResourceUtils.readFileFromURL(furl);
        // JOptionPane.showMessageDialog(null, contents);
        if(contents == null)
            return new Color[0][];
        ArrayList<Color[]> colorlines = new ArrayList<Color[]>();
        try {
            BufferedReader br = new BufferedReader(new StringReader(contents));
            for(String line = br.readLine(); line != null;) {
                StringTokenizer st = new StringTokenizer(line);
                Color[] colorlist = new Color[st.countTokens()];
                for(int i = 0; i < colorlist.length; i++) {
                    String colstr = st.nextToken();
                    colorlist[i] = PropertyManager.parseColor(colstr);
                    if(colorlist[i] == null) {
                        colorlist = null;
                        break; // bad line
                    }
                }
                if(colorlist != null)
                    colorlines.add(colorlist);
                line = br.readLine();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return colorlines.toArray(new Color[0][]);
    }

    /**
     * Performs a move and optionally applies it to the history when finished.
     */
    public void animate(MagicCube.TwistData move, boolean applyToHist, boolean macroMove) {
        animationQueue.append(move, applyToHist, macroMove);
        invalidate();
    }

    public void animate(MagicCube.TwistData move, boolean applyToHist) {
        animate(move, applyToHist, false);
    }

    // wants to be static
    private class AnimationQueue {
        private History queueHist;
        private Vector<Object> queue = new Vector<Object>();
        private QueueItem animating; // non-null == animation in progress

        private class QueueItem {
            public MagicCube.TwistData twist;
            public boolean applyAnimHistWhenDone = true; // whether to change history after animating
            public boolean macroMove = false;

            public QueueItem(MagicCube.TwistData twist, boolean applyAnimHistWhenDone, boolean macroMove) {
                this.twist = twist;
                this.applyAnimHistWhenDone = applyAnimHistWhenDone;
                this.macroMove = macroMove;
            }
        }

        public AnimationQueue(History hist) {
            queueHist = hist;
        }

        public MagicCube.TwistData getAnimating() {
            if(animating != null)
                return animating.twist;
            while(!queue.isEmpty()) {
                Object item = queue.remove(0);
                if(item instanceof QueueItem) { // this is an animatable item.
                    animating = (QueueItem) item;

                    int iTwistGrip = animating.twist.grip.id_within_puzzle;
                    int iSlicemask = animating.twist.slicemask;
                    int[] orders = puzzleManager.puzzleDescription.getGripSymmetryOrders();
                    if(0 > iTwistGrip || iTwistGrip >= orders.length) {
                        System.err.println("order indexing error in MC4CView.AnimationQueue.getAnimating()");
                        continue;
                    }
                    int order = orders[iTwistGrip];

                    if(!PipelineUtils.hasValidTwist(iTwistGrip, iSlicemask, puzzleManager.puzzleDescription))
                        continue;

                    double totalRotationAngle = 2 * Math.PI / order;
                    boolean quickly = false;
                    if(PropertyManager.getBoolean("quickmoves", false)) // use some form of quick moves
                        if(PropertyManager.getBoolean("quickmacros", false))
                            quickly = animating.macroMove;
                        else
                            quickly = true;
                    float speed = puzzleManager.puzzleDescription.getEdgeLength() < 3 ? 1 : .5f;
                    puzzleManager.nTwist = quickly ? 1 :
                            puzzleManager.calculateNTwists(totalRotationAngle, PropertyManager.getFloat("twistfactor", speed));
                    puzzleManager.iTwist = 0;
                    puzzleManager.iTwistGrip = iTwistGrip;
                    puzzleManager.twistDir = animating.twist.direction;
                    puzzleManager.twistSliceMask = animating.twist.slicemask;
                    break; // successfully dequeued a twist which is now animating.
                }
                if(item instanceof Character) // apply the queued mark and continue dequeuing.
                    queueHist.mark(((Character) item).charValue());
            }
            return animating == null ? null : animating.twist;
        } // end getAnimating

        public boolean isAnimating() {
            return animating != null;
        }

        public void append(MagicCube.TwistData twist, boolean applyAnimHistWhenDone, boolean macroMove) {
            queue.add(new QueueItem(twist, applyAnimHistWhenDone, macroMove));
            getAnimating(); // in case queue was empty this sets twist as animating
        }

        public void appendMark(char mark) {
            queue.add(new Character(mark));
        }

        public void finishedTwist() {
            if(animating != null && animating.applyAnimHistWhenDone)
                queueHist.apply(animating.twist);
            animating = null; // the signal that the twist is finished.
            getAnimating(); // queue up the next twist if any.
        }

        public void cancelAnimation() {
            animating = null;
            queue.removeAllElements();
        }
    } // end class AnimationQueue

}
