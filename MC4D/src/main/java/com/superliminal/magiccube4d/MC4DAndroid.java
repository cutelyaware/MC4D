package com.superliminal.magiccube4d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RadioButton;

import com.superliminal.magiccube4d.MagicCube.InputMode;
import com.superliminal.util.android.DialogUtils;

import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;

public class MC4DAndroid extends Activity {
    private static final int LENGTH = 3;
    private static final int FULLY = -1;
    private PuzzleManager mPuzzleManager;
    private History mHist = new History(LENGTH);
    private MC4DAndroidView view;
    private MediaPlayer mCorrectSound, mWipeSound, mWrongSound, mFanfareSound;

    private enum ScrambleState {
        NONE, FEW, FULL
    };

    private ScrambleState mScrambleState = ScrambleState.NONE;
    private boolean mIsScrambling = false;
    private boolean mHasAccelerometer = false;

    private SensorEventListener mShakeSensor = new SensorEventListener() {
        private static final double STRONG = 12;
        private long mLastStrongShake = System.currentTimeMillis();

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override
        public void onSensorChanged(SensorEvent event) {
            float force = (float) Math.sqrt(Vec_h._NORMSQRD3(event.values));
            long now = System.currentTimeMillis();
            long dur = now - mLastStrongShake;
            if(force > STRONG) {
                if(dur > 1500)
                    mLastStrongShake = now;
                else if(dur > 500) {
                    if(mHist.atScrambleBoundary()) { // Can't undo past scramble boundary.
                        playSound(mWrongSound);
                    }
                    else {
                        MagicCube.TwistData toUndo = mHist.undo();
                        if(toUndo != null) {
                            view.animate(toUndo, false);
                            view.invalidate();
                            playSound(mWipeSound);
                        }
                    }
                }
            }
        }
    };

    private void initMode(int id, final InputMode mode) {
        RadioButton rb = (RadioButton) findViewById(id);
        rb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                view.setInputMode(mode);
                int twistor_visibility = mode == InputMode.TWISTING ? View.VISIBLE : View.INVISIBLE;
                ((Button) findViewById(R.id.L)).setVisibility(twistor_visibility);
                ((Button) findViewById(R.id.R)).setVisibility(twistor_visibility);
            }
        });
    }

    private void initTwistor(int id, final int direction) {
        Button b = (Button) findViewById(id);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                view.twistSelected(direction);
            }
        });
    }

    private void initScrambler(int id, final int scramblechenfrengensen) {
        Button b = (Button) findViewById(id);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scramble(scramblechenfrengensen);
            }
        });
    }

    private static void playSound(MediaPlayer sound) {
        if(sound != null) // Because crash reports say this can happen.
            sound.start();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setContentView(R.layout.main); // For debugging only.
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        mPuzzleManager = new PuzzleManager(MagicCube.DEFAULT_PUZZLE, /* MagicCube.DEFAULT_LENGTH */LENGTH, new ProgressView());
        view = new MC4DAndroidView(getApplicationContext(), mPuzzleManager, mHist);
        //addContentView(view, params);
        ViewGroup holder = (ViewGroup) findViewById(R.id.puzzle_holder);
        (holder).addView(view, params);
        initMode(R.id.D3, InputMode.ROT_3D);
        initMode(R.id.D4, InputMode.ROT_4D);
        initMode(R.id.twisting, InputMode.TWISTING);
        view.setInputMode(InputMode.ROT_3D);

        initTwistor(R.id.L, 1);
        initTwistor(R.id.R, -1);

        initScrambler(R.id.scramble_1, 1);
        initScrambler(R.id.scramble_2, 2);
        initScrambler(R.id.scramble_3, 3);
        initScrambler(R.id.scramble_full, FULLY);

        Button b = (Button) findViewById(R.id.solve);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                solve();
            }
        });

        // TODO: Call release() on these resources when finished:
        mCorrectSound = MediaPlayer.create(getApplicationContext(), R.raw.correct);
        mWipeSound = MediaPlayer.create(getApplicationContext(), R.raw.wipe);
        mWrongSound = MediaPlayer.create(getApplicationContext(), R.raw.dink);
        mFanfareSound = MediaPlayer.create(getApplicationContext(), R.raw.fanfare);
        mHist.addHistoryListener(new History.HistoryListener() {
            private boolean adjusting = false;

            @Override
            public void currentChanged() {
                if(adjusting)
                    return; // Ignore messages from self.
                adjusting = true;
                if(mPuzzleManager.isSolved()) {
                    if(!(mScrambleState == ScrambleState.NONE || mIsScrambling))
                        if(mScrambleState == ScrambleState.FULL)
                            playSound(mFanfareSound); // Some poor sap solved full puzzle on Android?? Big reward.
                        else
                            playSound(mCorrectSound); // Small reward.
                    // Reset everything. Note: Won't want to reset history if we ever allow saving log files.
                    reset();
                }
                adjusting = false;
            }
        });
    }

//// Shake sensor UI doesn't work all that good so just don't use it.
//    @Override
//    protected void onResume() {
//        super.onResume();
//        SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
//        List<Sensor> sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
//        if(sensors.size() > 0) {
//            mHasAccelerometer = sensorMgr.registerListener(mShakeSensor, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        ((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(mShakeSensor);
//        super.onStop();
//    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.scramble1, menu);
//        inflater.inflate(R.menu.scramble2, menu);
//        inflater.inflate(R.menu.scramble3, menu);
//        inflater.inflate(R.menu.full, menu);
//        inflater.inflate(R.menu.solve, menu);
        inflater.inflate(R.menu.about, menu);
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item01:
                String appNameStr = getString(R.string.app_name) + " ";
                try {
                    appNameStr += "v" + getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName + " ";
                } catch(NameNotFoundException e) {
                    e.printStackTrace();
                }
                AlertDialog.Builder ab = new AlertDialog.Builder(MC4DAndroid.this);
                String html =
                    "<b><big>" + appNameStr + "</big><br>Copyright 2010 by Melinda Green <br>Superliminal Software</b><br>" +
                        "<hr width=\"100%\" align=\"center\" size=\"1\"> <br>" +
                        "This version of Magic Cube 4D is a mobile version of the full-featured desktop application " +
                        "from Superliminal.com. It lets you practice solving slightly randomized puzzles. " +
                        "The hyperstickers (small cubes) are also twisting control axes. " +
                        "Highlight one using the green pointer and click the left or right twist buttons to twist that face. " +
                        (mHasAccelerometer ? "Shake to undo your last twist. " : "") +
                        "<br><br>Send all questions and comments to <a href=\"mailto:feedback@superliminal.com\">feedback@superliminal.com</a>";
                DialogUtils.showHTMLDialog(this, html);
                break;
            case R.id.item02:
                scramble(1);
                break;
            case R.id.item03:
                scramble(2);
                break;
            case R.id.item04:
                scramble(3);
                break;
            case R.id.item05:
                scramble(FULLY);
                break;
            case R.id.item06:
                solve();
                break;
            default:
                break;
        }
        return true;
    } // end onOptionsItemSelected

    private void solve() {
        mScrambleState = ScrambleState.NONE; // User doesn't get credit for this solve.
        Stack<MagicCube.TwistData> toundo = new Stack<MagicCube.TwistData>();
        for(Enumeration<MagicCube.TwistData> moves = mHist.moves(); moves.hasMoreElements();)
            toundo.push(moves.nextElement());
        while(!toundo.isEmpty()) {
            MagicCube.TwistData last = toundo.pop();
            MagicCube.TwistData inv = new MagicCube.TwistData(last.grip, -last.direction, last.slicemask);
            view.animate(inv, true);
        }
    }


    private void reset() {
        mScrambleState = ScrambleState.NONE;
        mHist.clear();
        mPuzzleManager.resetPuzzleStateNoEvent();
        view.invalidate();
    }


    private void scramble(int scramblechenfrengensen) {
        mIsScrambling = true;
        reset();
        int previous_face = -1;
        PuzzleDescription puzzle = mPuzzleManager.puzzleDescription;
        int length = (int) puzzle.getEdgeLength();
        int totalTwistsNeededToFullyScramble =
                puzzle.nFaces() // needed twists is proportional to nFaces
                    * length // and to number of slices
                    * 2; // and to a fudge factor that brings the 3^4 close to the original 40 twists.
        int scrambleTwists = scramblechenfrengensen == -1 ? totalTwistsNeededToFullyScramble : scramblechenfrengensen;
        Random rand = new Random();
        for(int s = 0; s < scrambleTwists; s++) {
            // select a random grip that is unrelated to the last one (if any)
            int iGrip, iFace, order;
            do {
                // Generate a possible twist.
                iGrip = mPuzzleManager.getRandomGrip();
                iFace = puzzle.getGrip2Face()[iGrip];
                order = puzzle.getGripSymmetryOrders()[iGrip];
            } while( // Keep looking if any of the following conditions aren't met.
            (length > 2 ? (order < 2) : (order < 4)) || // Don't use 360 degree twists, and for 2^4 only allow 90 degree twists.
            (length > 2 && iFace == 7) || // Don't twist the invisible face since Android UI doesn't let the user do that either.
            iFace == previous_face || // Mixes it up better.
            (previous_face != -1 && puzzle.getFace2OppositeFace()[previous_face] == iFace));
            previous_face = iFace;
            int gripSlices = puzzle.getNumSlicesForGrip(iGrip);
            int slicemask = 1; //<<rand.nextInt(gripSlices);
            int dir = rand.nextBoolean() ? -1 : 1;
            // apply the twist to the puzzle state.
            puzzle.applyTwistToState(
                    mPuzzleManager.puzzleState,
                    iGrip,
                    dir,
                    slicemask);
            // and save it in the history.
            MagicCube.Stickerspec ss = new MagicCube.Stickerspec();
            ss.id_within_puzzle = iGrip; // slamming new id. do we need to set the other members?
            ss.face = puzzle.getGrip2Face()[iGrip];
            mHist.apply(ss, dir, slicemask);
            view.invalidate();
            //System.out.println("Adding scramble twist grip: " + iGrip + " dir: " + dir + " slicemask: " + slicemask);
        }
        mHist.mark(History.MARK_SCRAMBLE_BOUNDARY);
        mScrambleState = scramblechenfrengensen == -1 ? ScrambleState.FULL : ScrambleState.FEW;
        mIsScrambling = false;
    } // end scramble
}