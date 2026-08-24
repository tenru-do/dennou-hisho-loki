package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/** Reads only Gmail label counts. It does not read message subjects or bodies. */
public final class GmailUnreadReader {
    public static final String PERMISSION =
            "com.google.android.gm.permission.READ_CONTENT_PROVIDER";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String INBOX = "^i";

    private GmailUnreadReader() {
    }

    public static JSONObject read(Context context) {
        JSONObject result = new JSONObject();
        try {
            result.put("source", "gmail_label_provider");
            result.put("permissionGranted", context.checkSelfPermission(PERMISSION)
                    == PackageManager.PERMISSION_GRANTED);
            result.put("ok", false);
            result.put("unreadInbox", 0);
            result.put("accountsChecked", 0);
            if (context.checkSelfPermission(PERMISSION)
                    != PackageManager.PERMISSION_GRANTED) {
                result.put("status", "permission_required");
                return result;
            }

            Set<String> accounts = googleCalendarAccounts(context);
            int totalUnread = 0;
            int checked = 0;
            for (String account : accounts) {
                Integer unread = queryInboxUnread(context, account);
                if (unread == null) continue;
                totalUnread += Math.max(0, unread);
                checked++;
            }
            result.put("ok", checked > 0);
            result.put("unreadInbox", totalUnread);
            result.put("accountsChecked", checked);
            result.put("status", checked > 0 ? "ready" : "account_or_provider_unavailable");
        } catch (Exception error) {
            try {
                result.put("status", "query_failed");
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static Set<String> googleCalendarAccounts(Context context) {
        Set<String> accounts = new LinkedHashSet<String>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    CalendarContract.Calendars.CONTENT_URI,
                    new String[]{CalendarContract.Calendars.ACCOUNT_NAME,
                            CalendarContract.Calendars.ACCOUNT_TYPE},
                    CalendarContract.Calendars.ACCOUNT_TYPE + "=?",
                    new String[]{GOOGLE_ACCOUNT_TYPE}, null);
            if (cursor == null) return accounts;
            int nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);
            while (cursor.moveToNext()) {
                String account = nameIndex < 0 ? "" : cursor.getString(nameIndex);
                if (account != null && account.contains("@")) accounts.add(account);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return accounts;
    }

    private static Integer queryInboxUnread(Context context, String account) {
        Cursor cursor = null;
        try {
            Uri labels = new Uri.Builder().scheme("content")
                    .authority("com.google.android.gm")
                    .appendPath(account).appendPath("labels").build();
            cursor = context.getContentResolver().query(labels,
                    new String[]{"canonicalName", "numUnreadConversations"},
                    null, null, null);
            if (cursor == null) return null;
            int canonicalIndex = cursor.getColumnIndex("canonicalName");
            int unreadIndex = cursor.getColumnIndex("numUnreadConversations");
            if (canonicalIndex < 0 || unreadIndex < 0) return null;
            int categoryUnread = 0;
            boolean foundCategory = false;
            while (cursor.moveToNext()) {
                String canonical = cursor.getString(canonicalIndex);
                int unread = cursor.getInt(unreadIndex);
                if (INBOX.equals(canonical)) return unread;
                if (canonical != null && canonical.startsWith("^sq_ig_i_")) {
                    categoryUnread += Math.max(0, unread);
                    foundCategory = true;
                }
            }
            return foundCategory ? categoryUnread : null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
