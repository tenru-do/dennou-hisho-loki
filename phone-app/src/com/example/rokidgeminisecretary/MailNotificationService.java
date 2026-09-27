package com.example.rokidgeminisecretary;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MailNotificationService extends NotificationListenerService {
    private static final int MAX_ITEMS = 20;
    private static final long MAIL_CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final long TRANSIT_RESULT_MAX_AGE_MS = 15L * 60L * 1000L;
    private static final long ACTIVE_NAVIGATION_MAX_AGE_MS = 6L * 60L * 60L * 1000L;
    private static final String CACHE_PREFS = "mail_notification_cache";
    private static final String CACHE_KEY = "recent_mail_notifications";
    private static final Pattern NAVIGATION_DISTANCE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:[.,]\\d+)?)\\s*(キロメートル|メートル|km|ｋｍ|キロ|m|ｍ)(?:\\s*(先|後))?",
            Pattern.CASE_INSENSITIVE);
    private static final List<MailItem> MAILS = new ArrayList<MailItem>();
    private static HealthItem latestHealth;
    private static TransitItem latestTransit;
    private static volatile Notification latestMapsNotification;

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
            latestMapsNotification = null;
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
        String infoText = text(notification.extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        String body = bigText.length() > text.length() ? bigText : text;
        if (isGoogleMapsPackage(status.getPackageName())) {
            latestMapsNotification = notification;
            String expanded = expandedNavigationText(notification.extras);
            if (expanded.length() > 0 && !body.contains(expanded)) {
                body = body.length() == 0 ? expanded : body + " " + expanded;
            }
            collectTransit(status.getPackageName(), title, body, subText, infoText,
                    status.isOngoing(), notification.category, notification.extras);
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

    private static String expandedNavigationText(Bundle extras) {
        if (extras == null) return "";
        StringBuilder result = new StringBuilder();
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence line : lines) {
                appendUniqueNavigationText(result, text(line));
            }
        }
        appendUniqueNavigationText(result,
                text(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)));
        appendUniqueNavigationText(result,
                text(extras.getCharSequence(Notification.EXTRA_TITLE_BIG)));
        return result.toString();
    }

    private static void appendUniqueNavigationText(StringBuilder target, String value) {
        String clean = cleanTransitText(value);
        if (clean.length() == 0 || target.toString().contains(clean)) return;
        if (target.length() > 0) target.append(' ');
        target.append(clean);
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
            long age = latestTransit == null ? Long.MAX_VALUE
                    : System.currentTimeMillis() - latestTransit.time;
            boolean fresh = latestTransit != null
                    && latestTransit.compact.length() > 0
                    && age <= (latestTransit.navigationActive
                    ? ACTIVE_NAVIGATION_MAX_AGE_MS : TRANSIT_RESULT_MAX_AGE_MS);
            root.put("ok", fresh);
            root.put("compact", fresh ? latestTransit.compact : "");
            root.put("time", fresh ? latestTransit.time : 0L);
            root.put("source", fresh ? "Google Maps notification" : "");
            root.put("navigationActive", fresh && latestTransit.navigationActive);
            root.put("instruction", fresh ? latestTransit.instruction : "");
            root.put("detail", fresh ? latestTransit.detail : "");
            root.put("nextDistance", fresh ? latestTransit.nextDistance : "");
            root.put("arrival", fresh ? latestTransit.arrival : "");
            root.put("destination", fresh ? latestTransit.destination : "");
            root.put("travelMode", fresh ? NavigationLabels.mode(latestTransit.compact) : "");
            root.put("accessStation", fresh ? NavigationLabels.transitAccessStation(
                    latestTransit.instruction, latestTransit.detail) : "");
            root.put("accessMode", fresh ? NavigationLabels.accessMode(latestTransit.instruction) : "");
        }
        return root;
    }

    public static void clearTransit() {
        synchronized (MAILS) {
            latestTransit = null;
        }
    }

    static boolean stopMapsNavigation() {
        Notification current = latestMapsNotification;
        if (current == null || current.actions == null) return false;
        for (Notification.Action action : current.actions) {
            String title = action.title == null ? "" : action.title.toString();
            if (title.matches(".*(ナビを終了|ナビゲーションを終了|終了|Exit navigation|Stop navigation).*")) {
                try { action.actionIntent.send(); clearTransit(); return true; }
                catch (Exception ignored) { return false; }
            }
        }
        return false;
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

    private static void collectTransit(String packageName, String title, String body,
                                       String subText, String infoText,
                                       boolean ongoing, String category, Bundle extras) {
        NavigationText navigation = buildNavigationText(
                title, body, subText, infoText, ongoing, category, extras);
        String compact = navigation.compact;
        if (compact.length() == 0) {
            return;
        }
        synchronized (MAILS) {
            latestTransit = new TransitItem(System.currentTimeMillis(),
                    packageName == null ? "" : packageName, compact,
                    navigation.instruction, navigation.detail,
                    navigation.nextDistance, navigation.arrival,
                    NavigationLabels.destination(title, body, subText, infoText,
                            navigation.instruction, navigation.detail, navigation.arrival),
                    navigation.navigationActive);
        }
    }

    private static NavigationText buildNavigationText(String title, String body,
                                                       String subText, String infoText,
                                                       boolean ongoing, String category,
                                                       Bundle extras) {
        String cleanTitle = cleanTransitText(title);
        String cleanBody = cleanTransitText(body);
        String cleanSubText = cleanTransitText(subText);
        String cleanInfo = cleanTransitText(infoText);
        boolean titleTransit = looksLikeTransitGuidance(cleanTitle);
        boolean bodyTransit = looksLikeTransitGuidance(cleanBody);
        boolean subTransit = looksLikeTransitGuidance(cleanSubText);
        boolean infoTransit = looksLikeTransitGuidance(cleanInfo);
        boolean titleNavigation = looksLikeNavigationGuidance(cleanTitle);
        boolean bodyNavigation = looksLikeNavigationGuidance(cleanBody);
        boolean subNavigation = looksLikeNavigationGuidance(cleanSubText);
        boolean infoNavigation = looksLikeNavigationGuidance(cleanInfo);
        boolean navigationCategory = "navigation".equalsIgnoreCase(category);
        boolean navigationActive = navigationCategory || titleNavigation || bodyNavigation
                || subNavigation || infoNavigation
                || (ongoing && (titleTransit || bodyTransit || subTransit));
        boolean anyGuidance = titleTransit || bodyTransit || subTransit || infoTransit
                || navigationActive;
        if (!anyGuidance) {
            return NavigationText.EMPTY;
        }

        String instruction = "";
        if (titleNavigation || titleTransit) instruction = cleanTitle;
        if (instruction.length() == 0 && (bodyNavigation || bodyTransit)) instruction = cleanBody;
        if (instruction.length() == 0 && cleanTitle.length() > 0) instruction = cleanTitle;
        if (instruction.length() == 0 && cleanBody.length() > 0) instruction = cleanBody;
        if (instruction.length() == 0 && cleanSubText.length() > 0) instruction = cleanSubText;
        if (instruction.length() == 0 && cleanInfo.length() > 0) instruction = cleanInfo;

        String detail = "";
        String[] detailCandidates = {cleanBody, cleanSubText, cleanInfo, cleanTitle};
        for (String candidate : detailCandidates) {
            if (candidate.length() > 0 && !candidate.equals(instruction)) {
                detail = candidate;
                break;
            }
        }

        String arrival = finalArrivalText(cleanSubText, cleanInfo, cleanBody, cleanTitle);
        String nextDistance = nextActionDistance(
                cleanTitle, cleanBody, cleanSubText, cleanInfo);
        if (nextDistance.length() == 0) {
            nextDistance = progressNextActionDistance(extras);
        }

        String candidate;
        if (titleTransit && bodyTransit && !cleanBody.equals(cleanTitle)) {
            candidate = cleanTitle + " " + cleanBody;
        } else if (navigationActive && detail.length() > 0) {
            candidate = instruction + " " + detail;
        } else if (bodyTransit) {
            candidate = cleanBody;
        } else if (titleTransit) {
            candidate = cleanTitle;
        } else if (subTransit) {
            candidate = cleanSubText;
        } else {
            candidate = instruction;
        }
        return new NavigationText(shortText(candidate, 96),
                shortText(instruction, 72), shortText(detail, 72),
                shortText(nextDistance, 20), shortText(arrival, 40), navigationActive);
    }

    private static String nextActionDistance(String... candidates) {
        if (candidates == null) return "";
        boolean actionExists = false;
        for (String candidate : candidates) {
            if (hasDirectionalAction(candidate)) {
                actionExists = true;
                break;
            }
        }
        for (String candidate : candidates) {
            if (candidate == null || candidate.length() == 0) continue;
            Matcher matcher = NAVIGATION_DISTANCE_PATTERN.matcher(candidate);
            while (matcher.find()) {
                boolean directional = hasDirectionalAction(candidate);
                boolean ahead = matcher.group(3) != null && matcher.group(3).length() > 0;
                String remainder = (candidate.substring(0, matcher.start())
                        + candidate.substring(matcher.end()))
                        .replaceAll("[()（）・·,、。\\s]", "");
                boolean distanceOnly = remainder.length() <= 3 && !candidate.contains("分");
                boolean startsWithDistance = matcher.start() <= 4;
                if (!directional && !ahead && !distanceOnly
                        && !(startsWithDistance && actionExists)) {
                    continue;
                }
                String number = matcher.group(1).replace(',', '.');
                String rawUnit = matcher.group(2).toLowerCase();
                String unit = rawUnit.contains("k") || rawUnit.contains("ｋ")
                        || rawUnit.contains("キロ") ? "km" : "m";
                return number + " " + unit;
            }
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private static String progressNextActionDistance(Bundle extras) {
        if (extras == null) return "";
        try {
            int progress = extras.getInt("android.progress", -1);
            int progressMax = extras.getInt("android.progressMax", -1);
            ArrayList<Bundle> segments = extras.getParcelableArrayList(
                    "android.progressSegments");
            if (progress < 0 || progressMax <= 0 || segments == null
                    || segments.size() < 3) {
                return "";
            }
            long cumulative = 0L;
            for (Bundle segment : segments) {
                if (segment == null) continue;
                int length = segment.getInt("length", 0);
                if (length <= 0) continue;
                cumulative += length;
                // Very short leading segments represent the already-reached
                // route boundary in Google's ProgressStyle notification.
                if (cumulative <= progress + 3L) continue;
                long metres = cumulative - progress;
                if (metres <= 0L || metres > 100000L) return "";
                return formatNavigationDistance(metres);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String formatNavigationDistance(long metres) {
        if (metres >= 1000L) {
            double kilometres = metres / 1000.0;
            return String.format(java.util.Locale.JAPAN,
                    kilometres >= 10.0 ? "%.0f km" : "%.1f km", kilometres);
        }
        long rounded = metres >= 100L ? Math.max(10L, Math.round(metres / 10.0) * 10L)
                : metres;
        return rounded + " m";
    }

    private static boolean hasDirectionalAction(String value) {
        if (value == null || value.length() == 0) return false;
        String lower = value.toLowerCase();
        return value.contains("右折") || value.contains("左折")
                || value.contains("右方向") || value.contains("左方向")
                || value.contains("直進") || value.contains("曲が")
                || value.contains("Uターン") || value.contains("Ｕターン")
                || value.contains("出口") || value.contains("交差点")
                || value.contains("ロータリー") || value.contains("ラウンドアバウト")
                || value.contains("乗換") || value.contains("乗り換え")
                || value.contains("下車") || value.contains("次は")
                || lower.contains("turn right") || lower.contains("turn left")
                || lower.contains("continue") || lower.contains("exit");
    }

    private static String finalArrivalText(String... candidates) {
        if (candidates == null) return "";
        for (String candidate : candidates) {
            if (candidate == null || candidate.length() == 0) continue;
            String lower = candidate.toLowerCase();
            if (candidate.contains("着") || candidate.contains("到着")
                    || lower.contains("arrive") || lower.contains("arrival")) {
                return candidate;
            }
        }
        return "";
    }

    private static String cleanTransitText(String value) {
        return NavigationLabels.clean(value)
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

    private static boolean looksLikeNavigationGuidance(String value) {
        if (value == null || value.length() == 0) return false;
        String lower = value.toLowerCase();
        return value.contains("右折") || value.contains("左折")
                || value.contains("右方向") || value.contains("左方向")
                || value.contains("直進") || value.contains("曲が")
                || value.contains("Uターン") || value.contains("Ｕターン")
                || value.contains("目的地") || value.contains("到着")
                || value.contains("出口") || value.contains("交差点")
                || value.contains("ロータリー") || value.contains("ラウンドアバウト")
                || value.contains("経由") || value.contains("ルート")
                || value.contains("ナビ") || value.contains("案内")
                || value.matches(".*\\d+\\s*(m|ｍ|km|ｋｍ)\\s*(先|後)?.*")
                || lower.contains("turn right") || lower.contains("turn left")
                || lower.contains("continue") || lower.contains("destination")
                || lower.contains("arrive") || lower.contains("exit");
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
        final String instruction;
        final String detail;
        final String nextDistance;
        final String arrival;
        final String destination;
        final boolean navigationActive;

        TransitItem(long time, String packageName, String compact,
                    String instruction, String detail, String nextDistance, String arrival, String destination,
                    boolean navigationActive) {
            this.time = time;
            this.packageName = packageName;
            this.compact = compact;
            this.instruction = instruction;
            this.detail = detail;
            this.nextDistance = nextDistance;
            this.arrival = arrival;
            this.destination = destination;
            this.navigationActive = navigationActive;
        }
    }

    private static final class NavigationText {
        static final NavigationText EMPTY = new NavigationText("", "", "", "", "", false);
        final String compact;
        final String instruction;
        final String detail;
        final String nextDistance;
        final String arrival;
        final boolean navigationActive;

        NavigationText(String compact, String instruction, String detail,
                       String nextDistance, String arrival, boolean navigationActive) {
            this.compact = compact;
            this.instruction = instruction;
            this.detail = detail;
            this.nextDistance = nextDistance;
            this.arrival = arrival;
            this.navigationActive = navigationActive;
        }
    }
}
