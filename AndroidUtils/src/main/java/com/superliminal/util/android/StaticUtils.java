package com.superliminal.util.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * A somewhat general collection of small utility functions dealing with Android shared preferences
 * and other random tasks.
 * 
 * @author Melinda Green
 */
public class StaticUtils {
    static final String PREFS_NAME = "com.superliminal.utils";
    static final String PREF_KEY_PREFIX = "prefix_";
    static final int SHARE_PREFS_MODE = android.content.Context.MODE_PRIVATE;

    public static void saveStringPref(Context context, int id, String prefName, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE).edit();
        prefs.putString(StaticUtils.PREF_KEY_PREFIX + id + prefName, text);
        prefs.commit();
    }

    // Return the shared preference string for the given ID or null if not found.
    public static String loadStringPref(Context context, int id, String prefName) {
        SharedPreferences prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE);
        String value = prefs.getString(StaticUtils.PREF_KEY_PREFIX + id + prefName, null);
        return value;
    }

    // Write the given string to the shared preferences for a context and ID.
    public static void saveIntPref(Context context, int id, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE).edit();
        prefs.putString(StaticUtils.PREF_KEY_PREFIX + id, text);
        prefs.commit();
    }

    // Return the shared preference string for the given ID or null if not found.
    public static String loadIntPref(Context context, int id) {
        return loadIntPref(context, id, -1);
    }

    // Return the shared preference string for the given ID or of the ID of given default if none.
    public static String loadIntPref(Context context, int id, int defaultID) {
        String defaultStr = defaultID < 0 ? null : context.getString(defaultID);
        SharedPreferences prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE);
        String value = prefs.getString(StaticUtils.PREF_KEY_PREFIX + id, defaultStr);
        //Log.e("wind", "loadIntPref with " + id + ", " + defaultID + " returning: " + value);
        return value;
    }

    public static void deleteIntPrefs(Context context, int[] ints) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE).edit();
        for(int id : ints) {
            prefs.remove(StaticUtils.PREF_KEY_PREFIX + id);
        }
    }

    public static void deleteAllIntPrefs(Context context) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(StaticUtils.PREFS_NAME, StaticUtils.SHARE_PREFS_MODE).edit();
        prefs.clear();
        prefs.commit();
    }

    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch(Exception e) {
            Thread.dumpStack();
            return null;
        }
    }

    public static String guessMimeType(String fname) {
        String type = URLConnection.guessContentTypeFromName(fname);
        // Note: The above can fail on pre-gingerbread devices.
        // See bug:http://code.google.com/p/android/issues/detail?id=10100
        // The following fixes the more important cases.
        if(type == null && fname != null) {
            if(fname.toLowerCase().endsWith(".jpg"))
                type = "image/jpeg";
            else if(fname.toLowerCase().endsWith(".mp4"))
                type = "video/mp4";
        }
        return type;
    }

    public static Intent createFileViewIntent(File f) throws IOException {
        Intent intent;
        Uri uri = Uri.fromFile(f);
        String type = guessMimeType(uri.toString());
        if(type == null)
            return null;
        Log.d("share", String.format("%s is of type %s", uri, type));
        if(f.isDirectory()) {
            intent = new Intent();
            intent.setData(uri);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type);
        }
        return intent;
    }

    public static String getFileType(String s) {
        return getFileType(new File(s));
    }

    public static String getFileType(File f) {
        Uri uri = Uri.fromFile(f);
        String type = guessMimeType(uri.toString());
        if(type == null)
            return "folder";
        String[] parts = type.split("/");
        if(parts.length == 0)
            return "folder";
        return parts[0];
    }

    public static String now2str() {
        return (new SimpleDateFormat("yyyyMMdd_HHmmss")).format(new Date());
    }

    /**
     * Converts the given URI to its direct file system path.
     * From http://www.androidsnippets.com/get-file-path-of-gallery-image.
     * Modified to also handle non-image types.
     */
    public static String getRealPathFromURI(Uri contentUri, Activity act) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = act.managedQuery(contentUri,
             proj, // Which columns to return
             null, // WHERE clause; which rows to return (all rows)
             null, // WHERE clause selection arguments (none)
             null); // Order-by clause (ascending by name)
        if(cursor == null)
            return contentUri.getPath();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

//    public static class TextWatcherStub implements TextWatcher {
//        @Override
//        public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//        @Override
//        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//        @Override
//        public void afterTextChanged(Editable s) {}
//    }
//
//    public static class ItemSelectedListenerStub implements OnItemSelectedListener {
//        @Override
//        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {}
//
//        @Override
//        public void onNothingSelected(AdapterView<?> parent) {}
//    }
}
