package com.superliminal.util.android;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

public class DialogUtils {
    // See bug report: http://code.google.com/p/android/issues/detail?id=2219
    // From workaround at http://stackoverflow.com/questions/1997328/android-clickable-hyperlinks-in-alertdialog
    public static void showHTMLDialog(Context context, String html) {
        // Linkify the message
        final SpannableString s = new SpannableString(Html.fromHtml(html, FROM_HTML_MODE_LEGACY));
        Linkify.addLinks(s, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        final AlertDialog d = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, null)
                //.setIcon(R.drawable.icon)
                .setMessage(s)
                .create();
        d.show();
        // Make the textview clickable. Must be called after show()
        ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
    
}
