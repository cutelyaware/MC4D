package com.superliminal.util.android;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class EmailUtils {

    public static void sendEmail(String to, String subject, String body, Context cxt) {
        final Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
        sendIntent.setType("plain/text");
        sendIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{to});
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        cxt.startActivity(Intent.createChooser(sendIntent, "Send mail..."));
    }

    /**
     * @return A list of email addresses probably belonging to the user.
     */
    public static ArrayList<String> getEmails(Context c) {
        Pattern rfc2822 = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
        Account[] accounts = AccountManager.get(c).getAccounts();
        ArrayList<String> emails = new ArrayList<String>();
        for(Account account : accounts)
            if(rfc2822.matcher(account.name).matches())
                emails.add(account.name);
        return emails;
    }

    /**
     * Packs a name+email pair into the form "name<email>" form
     */
    public static String packNameAddressPair(String name, String address) {
        return name + "<" + address + ">";
    }

    /**
     * Unpacks a possible "name<email>" form address into a [name,email] array.
     * If either or both angle brackets is missing, the returned name is empty
     * and the returned address is the input string.
     */
    public static String[] unpackNameAddressPair(String packed) {
        String[] ret = new String[2]; // {name,address}
        int openBracketLoc = packed.indexOf('<');
        int closeBracketLoc = packed.indexOf('>');
        if(openBracketLoc < 0 || closeBracketLoc < 0) {
            ret[1] = packed; // Address must be the whole string.
        } else {
            ret[0] = packed.substring(0, openBracketLoc);
            ret[1] = packed.substring(openBracketLoc + 1, closeBracketLoc);
        }
        return ret;
    }

    /**
     * @return A comma-separated String version of the given String array.
     */
    public static String list2string(String[] recipients) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < recipients.length; i++) {
            builder.append(recipients[i]);
            if(i < recipients.length - 1)
                builder.append(", "); // Add comma separators but not at the very end.
        }
        return builder.toString();
    }

    /**
     * @return A pure data cursor for a list of strings with an optional filter
     *         such that only rows containing the given string will be included.
     */
    public static Cursor buildFilteredStringListCursor(String[] strings, String filter) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "display_name"});
        int row = 0;
        for(String string : strings)
            if(filter == null || string.toLowerCase().contains(filter.toLowerCase()))
                cursor.addRow(new Object[]{"" + row++, string});
        return cursor;
    }

    /**
     * Base implementation of contacts search.
     * 
     * @return A managed cursor of contacts for the given activity and optional String prefix.
     */
    public static Cursor buildFilteredCursor(Activity activity, String prefix, String dataKind, Uri uri) {
        String my_sort_order = null;
        String my_selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";
        if(prefix != null)
            my_selection += " AND " + ContactsContract.Contacts.DISPLAY_NAME + " LIKE '" + prefix + "%'";
        String[] eproj = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            dataKind};
        return activity.managedQuery(uri, eproj, my_selection, null, my_sort_order);
    }

    public static Cursor buildFilteredEmailCursor(Activity activity, String prefix) {
        Uri email_uri = android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String email_kind = ContactsContract.CommonDataKinds.Email.DATA;
        return buildFilteredCursor(activity, prefix, email_kind, email_uri);
    }

    public static Cursor buildFilteredPhoneCursor(Activity activity, String prefix) {
        Uri phone_uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String phone_kind = ContactsContract.CommonDataKinds.Phone.DATA;
        return buildFilteredCursor(activity, prefix, phone_kind, phone_uri);
    }

}