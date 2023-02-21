package com.superliminal.magiccube4d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.os.Build;
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
import com.superliminal.util.android.EmailUtils;
import com.superliminal.util.android.Graphics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

public class MC4DAndroid extends Activity {
    private static final int EDGE_LENGTH = 3;
    private static final int FULLY = -1;
    private PuzzleManager mPuzzleManager;
    private History mHist = new History(EDGE_LENGTH);
    private MC4DAndroidView view;
    private MediaPlayer mCorrectSound, mWipeSound, mWrongSound, mFanfareSound;
    private enum ScrambleState { NONE, FEW, FULL }
    private ScrambleState mScrambleState = ScrambleState.NONE;
    private boolean mIsScrambling = false;

    private void initMode(int id, final InputMode mode) {
        RadioButton rb = (RadioButton) findViewById(id);
        rb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                view.setInputMode(mode);
                int twistor_visibility = mode == InputMode.TWISTING ? View.VISIBLE : View.INVISIBLE;
                findViewById(R.id.L).setVisibility(twistor_visibility);
                findViewById(R.id.R).setVisibility(twistor_visibility);
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
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPuzzleManager = new PuzzleManager(MagicCube.DEFAULT_PUZZLE, /* MagicCube.DEFAULT_LENGTH */EDGE_LENGTH, new ProgressView());
        File log_file = new File(getFilesDir(), MagicCube.LOG_FILE);
        view = new MC4DAndroidView(getApplicationContext(), mPuzzleManager, mHist);
        boolean readOK = readAndApplyLog(log_file);
        //addContentView(view, params);
        ViewGroup holder = (ViewGroup) findViewById(R.id.puzzle_holder);
        holder.addView(view, params);
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
                writeLog(new File(getFilesDir(), MagicCube.LOG_FILE));
                if(mPuzzleManager.isSolved()) {
                    if(!(mScrambleState == ScrambleState.NONE || mIsScrambling))
                        if(mScrambleState == ScrambleState.FULL)
                            playSound(mFanfareSound); // Some poor sap solved full puzzle on Android?? Big reward.
                        else
                            playSound(mCorrectSound); // Small reward.
                    // Reset puzzle state.
                    mScrambleState = ScrambleState.NONE;
                }
                adjusting = false;
            }
        });
    }

//// Shake sensor UI doesn't work all that good so just don't use it.
//private boolean mHasAccelerometer = false;
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
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if(itemId == R.id.itemAbout) {
            String appNameStr = getString(R.string.app_name) + " ";
            try {
                String versionName;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    versionName = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.PackageInfoFlags.of(0)).versionName;
                } else {
                    versionName = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
                }
                appNameStr += "v" + versionName + " ";
            } catch(NameNotFoundException e) {
                e.printStackTrace();
            }
            String html =
                    "<b><big>" + appNameStr + "</big><br>Copyright 2010 by Melinda Green <br>Superliminal Software</b><br>" +
                            "<hr width=\"100%\" align=\"center\" size=\"1\"> <br>" +
                            "This version of Magic Cube 4D is a mobile version of the full-featured desktop application " +
                            "from Superliminal.com. It lets you practice solving slightly randomized puzzles. " +
                            "The hyperstickers (small cubes) are also twisting control axes. " +
                            "Highlight one using the green pointer and click the left or right twist buttons to twist that face. " +
                            "<br><br>Send all questions and comments to <a href=\"mailto:feedback@superliminal.com\">feedback@superliminal.com</a>";
            DialogUtils.showHTMLDialog(this, html);
        } else if(itemId == R.id.itemScramble1) {
            scramble(1);
        } else if(itemId == R.id.itemScramble2) {
            scramble(2);
        } else if(itemId == R.id.itemScramble3) {
            scramble(3);
        } else if(itemId == R.id.itemScrambleFull) {
            scramble(FULLY);
        } else if(itemId == R.id.itemSolve) {
            solve();
        } else if(itemId == R.id.itemSendLog) {
            sendLog(new File(getFilesDir(), MagicCube.LOG_FILE));
        } else if(itemId == R.id.itemPuzzle2222) {
            changePuzzle(2);
        } else if(itemId == R.id.itemPuzzle3333) {
            changePuzzle(3);
        } else {
            return false;
        }
        return true;
    } // end onOptionsItemSelected

    private void changePuzzle(int length) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.change_puzzle_warning)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                mHist.clear(length);
                mPuzzleManager.initPuzzle(MagicCube.DEFAULT_PUZZLE, "" + length, new ProgressView(), new Graphics.Label(), false);
                reset();
            })
            .setNegativeButton(R.string.no, (dialog, which) -> {
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void solve() {
        mScrambleState = ScrambleState.NONE; // User doesn't get credit for this solve.
        Stack<MagicCube.TwistData> toundo = new Stack<>();
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

    /*
     * Format:
     * 0 - Magic Number
     * 1 - File Version
     * 2 - Scramble State
     * 3 - Twist Count
     * 4 - Schlafli Product
     * 5 - Edge Length
     */
    private void writeLog(File logfile) {
        String sep = System.getProperty("line.separator");
        int scrambleStateInt = mScrambleState == ScrambleState.FULL ? 2 : mScrambleState == ScrambleState.FEW ? 1 : 0;
        try {
            Writer writer = new FileWriter(logfile);
            writer.write(
                    MagicCube.MAGIC_NUMBER + " " +
                            MagicCube.LOG_FILE_VERSION + " " +
                            scrambleStateInt + " " +
                            mHist.countTwists() + " " +
                            mPuzzleManager.puzzleDescription.getSchlafliProduct() + " " +
                            mPuzzleManager.getPrettyLength());
            writer.write(sep);
            view.getRotations().write(writer);
            //writer.write(sep + puzzle.toString());
            writer.write("*" + sep);
            mHist.write(writer);
            writer.close();
            String filepath = logfile.getAbsolutePath();
            //PropertyManager.userprefs.setProperty("logfile", filepath);
            //setTitle(MagicCube.TITLE + " - " + logfile.getName());
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    } // end writeLog()

    private boolean readAndApplyLog(File logfile) {
        if( ! logfile.exists())
            return false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logfile));
            String firstlineStr = reader.readLine();
            if(firstlineStr == null)
                throw new IOException("Empty log file.");
            String firstline[] = firstlineStr.split(" ");
            if(firstline.length != 6 || !MagicCube.MAGIC_NUMBER.equals(firstline[0]))
                throw new IOException("Unexpected log file format.");
            int readversion = Integer.parseInt(firstline[1]);
            if(readversion != MagicCube.LOG_FILE_VERSION) {
                return false;
            }
            int state = Integer.parseInt(firstline[2]);
            mScrambleState = state == 2 ? ScrambleState.FULL : state == 1 ? ScrambleState.FEW : ScrambleState.NONE;
            int numTwists = Integer.parseInt(firstline[3]);
            String schlafli = firstline[4];
            double initialLength = Double.parseDouble(firstline[5]);
            mPuzzleManager.initPuzzle(schlafli, "" + initialLength,  new ProgressView(), new Graphics.Label(), false);
            int iLength = (int) Math.round(initialLength);
            view.getRotations().read(reader);
            String title = MagicCube.TITLE;
            for(int c = reader.read(); !(c == '*' || c == -1); c = reader.read())
                ; // read past state data
            if(mHist.read(new PushbackReader(reader))) {
                title += " - " + logfile.getName();
                for(Enumeration<MagicCube.TwistData> moves = mHist.moves(); moves.hasMoreElements();) {
                    MagicCube.TwistData move = moves.nextElement();
                    if(move.grip.id_within_puzzle == -1) {
                        System.err.println("Bad move in MC4DAndroid.initPuzzle: " + move.grip.id_within_puzzle);
                        return false;
                    }
                    mPuzzleManager.puzzleDescription.applyTwistToState(
                            mPuzzleManager.puzzleState,
                            move.grip.id_within_puzzle,
                            move.direction,
                            move.slicemask);
                }
            }
            else
                System.out.println("Error reading puzzle history");
            //setTitle(title);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    } // end readAndApplyLog()

    private boolean sendLog(File logfile) {
        if(!logfile.exists())
            return false;
        String text = "";
        try {
            text = new Scanner(logfile).useDelimiter("\\A").next();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        EmailUtils.sendEmail(null, "MagicCube4D log file", text, this);
        return true;
    }

//    private SensorEventListener mShakeSensor = new SensorEventListener() {
//        private static final double STRONG = 12;
//        private long mLastStrongShake = System.currentTimeMillis();
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            float force = (float) Math.sqrt(Vec_h._NORMSQRD3(event.values));
//            long now = System.currentTimeMillis();
//            long dur = now - mLastStrongShake;
//            if(force > STRONG) {
//                if(dur > 1500)
//                    mLastStrongShake = now;
//                else if(dur > 500) {
//                    if(mHist.atScrambleBoundary()) { // Can't undo past scramble boundary.
//                        playSound(mWrongSound);
//                    }
//                    else {
//                        MagicCube.TwistData toUndo = mHist.undo();
//                        if(toUndo != null) {
//                            view.animate(toUndo, false);
//                            view.invalidate();
//                            playSound(mWipeSound);
//                        }
//                    }
//                }
//            }
//        }
//    };

}