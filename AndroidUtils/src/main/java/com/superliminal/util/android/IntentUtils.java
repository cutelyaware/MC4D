package com.superliminal.util.android;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * In the voice of the wicked witch of the west:
 * Creating and launching Intents with app IDs must be done "del-i-cate-ly".
 * 
 * @author Melinda Green - Superliminal Software
 */
public class IntentUtils {

    /**
     * Returns an Intent for launching a given target class
     * from a given source context, and containing the given app ID.
     */
    public static Intent createIntentWithAppIDExtra(int widgetID, Context source, Class<?> targetClass) {
        Intent intent = new Intent(source, targetClass);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Crucial: When intents are compared, the extras are ignored, so we need to make it unique.
        // The recommended way is to encode the intent as a URI and insert it into the data field
        // so that the extras data will not be ignored. This critical bit of arcanum shown hereP
        // http://docs.huihoo.com/android/3.0/resources/samples/HoneycombGallery/src/com/example/android/hcgallery/widget/WidgetProvider.html
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        return intent;
    }

    /**
     * Launches an Intent for launching a given target class
     * from a given source Activity, and containing that Activity's widget ID.
     */
    public static void launchAppWithAppIDExtra(Activity srcActivity, Class<?> targetClass) {
        Intent srcIntent = srcActivity.getIntent();
        Bundle bundle = srcIntent.getExtras();
        int widgetID = bundle.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if(widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            //TODO: handle error
        }
        Intent destIntent = createIntentWithAppIDExtra(widgetID, srcActivity, targetClass);
        srcActivity.startActivity(destIntent);
    }

}
