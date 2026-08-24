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
    private static final long MAIL_CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String CACHE_PREFS = "mail_notification_cache";
    private static final String CACHE_KEY = "recent_mail_notifications";
    private static final List<MailItem> MAILS = new ArrayList<MailItem>();
    private static HealthItem latestHealth;
    private static TransitItem latestTransit;

    @Override
    public void onCreate() {
        super.onCreate();
        loadMailCache();
    }

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

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (notification == null || !isGoogleMapsPackage(notification.getPackageName())) {
            return;
        }
        synchronized (MAILS) {
            latestTransit = null;
        }
        StatusBarNotification[] notifications = getActiveNotifications();
        if (notifications == null) return;
        for (StatusBarNotification active : notifications) {
            if (active != null && isGoogleMapsPackage(active.getPackageName())) {
                collect(active);
            }
        }
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

    private static boolean isGoogleMapsPackage(String packageName) {
        return "com.google.android.apps.maps".equals(packageName);
    }

    private void collect(StatusBarNotification status) {
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
        if (isGoogleMapsPackage(status.getPackageName())) {
            collectTransit(status.getPackageName(), title, body, subText);
            return;
        }
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
        saveMailCache();
    }

    private void loadMailCache() {
        long cutoff = System.currentTimeMillis() - MAIL_CACHE_MAX_AGE_MS;
        try {
            JSONArray cached = new JSONArray(getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                    .getString(CACHE_KEY, "[]"));
            synchronized (MAILS) {
                MAILS.clear();
                for (int index = 0; index < cached.length() && MAILS.size() < MAX_ITEMS; index++) {
                    JSONObject value = cached.optJSONObject(index);
                    if (value == null) continue;
                    long time = value.optLong("time", 0L);
                    if (time < cutoff) continue;
                    String packageName = value.optString("app", "");
                    String title = value.optString("title", "");
                    String body = value.optString("body", "");
                    String subText = value.optString("subText", "");
                    if (!isMailPackage(packageName) || (title.length() == 0 && body.length() == 0)) {
                        continue;
                    }
                    MAILS.add(new MailItem(time, packageName, title, body, subText));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveMailCache() {
        try {
            JSONArray cached = new JSONArray();
            long cutoff = System.currentTimeMillis() - MAIL_CACHE_MAX_AGE_MS;
            synchronized (MAILS) {
                for (MailItem item : MAILS) {
                    if (item.time < cutoff) continue;
                    JSONObject value = new JSONObject();
                    value.put("time", item.time);
                    value.put("app", item.packageName);
                    value.put("title", item.title);
                    value.put("body", item.body);
                    value.put("subText", item.subText);
                    cached.put(value);
                }
            }
            getSharedPreferences(CACHE_PREFS, MODE_PRIVATE).edit()
                    .putString(CACHE_KEY, cached.toString()).apply();
        } catch (Exception ignored) {
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
                // MorningBriefingManager uses this composite only as a stable
                // deduplication key; the spoken fields above remain unchanged.
                mail.put("title", shortText(item.title + " " + item.body, 240));
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

    public static JSONObject recentTransitJson() throws Exception {
        JSONObject root = new JSONObject();
        synchronized (MAILS) {
            boolean fresh = latestTransit != null
                    && latestTransit.compact.length() > 0
                    && System.currentTimeMillis() - latestTransit.time <= 900000L;
            root.put("ok", fresh);
            root.put("compact", fresh ? latestTransit.compact : "");
            root.put("time", fresh ? latestTransit.time : 0L);
            root.put("source", fresh ? "Google Maps notification" : "");
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

    private static void collectTransit(String packageName, String title, String body, String subText) {
        String compact = compactTransitText(title, body, subText);
        if (compact.length() == 0) {
            return;
        }
        synchronized (MAILS) {
            latestTransit = new TransitItem(System.currentTimeMillis(),
                    packageName == null ? "" : packageName, compact);
        }
    }

    private static String compactTransitText(String title, String body, String subText) {
        String cleanTitle = cleanTransitText(title);
        String cleanBody = cleanTransitText(body);
        String cleanSubText = cleanTransitText(subText);
        boolean titleTransit = looksLikeTransitGuidance(cleanTitle);
        boolean bodyTransit = looksLikeTransitGuidance(cleanBody);
        boolean subTransit = looksLikeTransitGuidance(cleanSubText);
        String candidate;
        if (titleTransit && bodyTransit && !cleanBody.equals(cleanTitle)) {
            candidate = cleanTitle + " " + cleanBody;
        } else if (bodyTransit) {
            candidate = cleanBody;
        } else if (titleTransit) {
            candidate = cleanTitle;
        } else if (subTransit) {
            candidate = cleanSubText;
        } else {
            return "";
        }
        return shortText(candidate, 42);
    }

    private static String cleanTransitText(String value) {
        return (value == null ? "" : value)
                .replace("Google マップ", "")
                .replace("Google Maps", "")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeTransitGuidance(String value) {
        if (value == null || value.length() == 0) return false;
        return value.contains("駅")
                || value.contains("停留所")
                || value.contains("次は")
                || value.contains("次の停車")
                || value.contains("乗換")
                || value.contains("乗り換え")
                || value.contains("下車")
                || value.contains("番線")
                || value.contains("ホーム");
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

    private static final class TransitItem {
        final long time;
        final String packageName;
        final String compact;

        TransitItem(long time, String packageName, String compact) {
            this.time = time;
            this.packageName = packageName;
            this.compact = compact;
        }
    }
}
