package com.example.rokidgeminisecretary;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MailNotificationService extends NotificationListenerService {
    private static final int MAX_ITEMS = 20;
    private static final List<MailItem> MAILS = new ArrayList<MailItem>();
    private static HealthItem latestHealth;

    @Override
    public void onListenerConnected() {
        StatusBarNotification[] notifications = getActiveNotifications();
        if (notifications == null) return;
        for (StatusBarNotification notification : notifications) {
            collect(notification);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        collect(notification);
    }

    private static boolean isMailPackage(String packageName) {
        return "com.google.android.gm".equals(packageName)
                || "com.google.android.apps.inbox".equals(packageName)
                || "com.microsoft.office.outlook".equals(packageName)
                || "com.yahoo.mobile.client.android.mail".equals(packageName)
                || "jp.co.yahoo.android.ymail".equals(packageName);
    }

    private static boolean isHealthPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        String lower = packageName.toLowerCase();
        return lower.contains("healbe") || lower.contains("gobe");
    }

    private static void collect(StatusBarNotification status) {
        if (status == null) {
            return;
        }
        Notification notification = status.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }
        String title = text(notification.extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = text(notification.extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = text(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String subText = text(notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        String body = bigText.length() > text.length() ? bigText : text;
        if (isHealthPackage(status.getPackageName()) || looksLikeHealth(title, body, subText)) {
            collectHealth(status.getPostTime(), status.getPackageName(), title, body, subText);
            return;
        }
        if (!isMailPackage(status.getPackageName())) {
            return;
        }
        if (title.length() == 0 && body.length() == 0) {
            return;
        }
        MailItem item = new MailItem(status.getPostTime(), status.getPackageName(),
                shortText(title, 80), shortText(body, 160), shortText(subText, 60));
        synchronized (MAILS) {
            for (int i = MAILS.size() - 1; i >= 0; i--) {
                MailItem existing = MAILS.get(i);
                if (existing.packageName.equals(item.packageName)
                        && existing.title.equals(item.title)
                        && existing.body.equals(item.body)) {
                    MAILS.remove(i);
                }
            }
            MAILS.add(0, item);
            while (MAILS.size() > MAX_ITEMS) {
                MAILS.remove(MAILS.size() - 1);
            }
        }
    }

    public static JSONArray recentMailJson() throws Exception {
        JSONArray mails = new JSONArray();
        synchronized (MAILS) {
            for (MailItem item : MAILS) {
                JSONObject mail = new JSONObject();
                mail.put("time", item.time);
                mail.put("app", item.packageName);
                mail.put("from_or_title", item.title);
                mail.put("summary", item.body);
                mail.put("account", item.subText);
                mails.put(mail);
            }
        }
        return mails;
    }

    public static JSONObject recentHealthJson() throws Exception {
        JSONObject root = new JSONObject();
        synchronized (MAILS) {
            if (latestHealth == null || latestHealth.compact.length() == 0) {
                root.put("ok", false);
                root.put("compact", "");
                root.put("time", 0L);
                root.put("source", "");
                return root;
            }
            root.put("ok", true);
            root.put("compact", latestHealth.compact);
            root.put("time", latestHealth.time);
            root.put("source", latestHealth.packageName);
        }
        return root;
    }

    private static boolean looksLikeHealth(String title, String body, String subText) {
        String combined = ((title == null ? "" : title) + " " + (body == null ? "" : body) + " " + (subText == null ? "" : subText)).toLowerCase();
        return combined.contains("healbe")
                || combined.contains("gobe")
                || combined.contains("hydration")
                || combined.contains("energy balance")
                || combined.contains("calorie intake");
    }

    private static void collectHealth(long time, String packageName, String title, String body, String subText) {
        String compact = compactHealthText(title, body, subText);
        if (compact.length() == 0) {
            return;
        }
        synchronized (MAILS) {
            latestHealth = new HealthItem(time, packageName == null ? "" : packageName, compact);
        }
    }

    private static String compactHealthText(String title, String body, String subText) {
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body) + " " + (subText == null ? "" : subText))
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        text = text.replace("HEALBE", "")
                .replace("Healbe", "")
                .replace("GoBe", "")
                .replace("GOBE", "")
                .replace("healbe", "")
                .trim();
        if (text.length() == 0) {
            return "";
        }
        return "HEALBE " + shortText(text, 54);
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String shortText(String value, int max) {
        String text = value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (TextUtils.isEmpty(text) || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

    private static final class MailItem {
        final long time;
        final String packageName;
        final String title;
        final String body;
        final String subText;

        MailItem(long time, String packageName, String title, String body, String subText) {
            this.time = time;
            this.packageName = packageName;
            this.title = title;
            this.body = body;
            this.subText = subText;
        }
    }

    private static final class HealthItem {
        final long time;
        final String packageName;
        final String compact;

        HealthItem(long time, String packageName, String compact) {
            this.time = time;
            this.packageName = packageName;
            this.compact = compact;
        }
    }
}
