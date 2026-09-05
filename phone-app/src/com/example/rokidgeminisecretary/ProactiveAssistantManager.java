package com.example.rokidgeminisecretary;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds a small, local-first context cache and emits short proactive alerts.
 * This deliberately does not call Gemini, so calendar/rain/transit notices do
 * not consume model quota and continue to be useful during an API cooldown.
 */
final class ProactiveAssistantManager {
    private static final String TAG = "LokiProactive";
    private static final String PREFS = "phone_secretary";
    private static final String KEY_CACHE = "proactive_offline_cache";
    private static final String KEY_CACHE_SENT = "proactive_offline_cache_sent";
    private static final String KEY_CACHE_SENT_AT = "proactive_offline_cache_sent_at";
    private static final String KEY_LAST_EVENT_ALERT = "proactive_last_event_alert";
    private static final String KEY_LAST_RAIN_ALERT = "proactive_last_rain_alert";
    private static final String KEY_LAST_TRANSIT_ALERT = "proactive_last_transit_alert";
    private static final String KEY_RAIN_HOURLY = "proactive_rain_hourly";
    private static final String KEY_RAIN_UPDATED_AT = "proactive_rain_updated_at";
    private static final String KEY_WEATHER_LOCATION = "weather_location";
    private static final long RAIN_REFRESH_MS = 15L * 60L * 1000L;
    private static final long TRANSIT_FRESH_MS = 12L * 60L * 1000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private ProactiveAssistantManager() {}

    static void runAsync(final Context context) {
        if (!RUNNING.compareAndSet(false, true)) return;
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    runNow(app);
                } catch (Exception error) {
                    Log.w(TAG, "proactive pass failed", error);
                } finally {
                    RUNNING.set(false);
                }
            }
        }, "LokiProactive").start();
    }

    private static void runNow(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        JSONArray events = readUpcomingEvents(context, now);
        refreshRainIfNeeded(context, prefs, now);
        JSONArray rain = readJsonArray(prefs.getString(KEY_RAIN_HOURLY, "[]"));
        JSONObject cache = buildOfflineCache(prefs, events, rain, now);
        String cacheText = cache.toString();
        prefs.edit().putString(KEY_CACHE, cacheText).apply();

        JSONObject alert = scheduleAlert(prefs, events, now);
        if (alert == null) alert = rainAlert(prefs, rain, now);
        if (alert == null) alert = transitAlert(context, prefs, now);
        if (alert != null) {
            if (MainActivity.queueBridgeCommand(context,
                    "__LOKI_ALERT__:" + alert.toString(), false)) {
                rememberAlert(prefs, alert);
            }
            return;
        }

        String lastSent = prefs.getString(KEY_CACHE_SENT, "");
        if ((lastSent.length() == 0
                || now - prefs.getLong(KEY_CACHE_SENT_AT, 0L) >= 5L * 60L * 1000L)
                && MainActivity.queueBridgeCommand(context,
                "__LOKI_CACHE__:" + cacheText, false)) {
            prefs.edit().putString(KEY_CACHE_SENT, cacheText)
                    .putLong(KEY_CACHE_SENT_AT, now).apply();
        }
    }

    private static JSONArray readUpcomingEvents(Context context, long now) throws Exception {
        JSONArray events = new JSONArray();
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) return events;
        long end = now + 36L * 60L * 60L * 1000L;
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.setTimeInMillis(now);
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startOfToday.getTimeInMillis());
        ContentUris.appendId(builder, end);
        String[] projection = new String[] {
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
        };
        Cursor cursor = context.getContentResolver().query(builder.build(), projection,
                CalendarContract.Instances.VISIBLE + "!=0", null,
                CalendarContract.Instances.BEGIN + " ASC");
        if (cursor == null) return events;
        try {
            while (cursor.moveToNext() && events.length() < 10) {
                long begin = cursor.getLong(2);
                long finish = cursor.getLong(3);
                boolean allDay = cursor.getInt(4) != 0;
                if (!allDay && finish < now) continue;
                JSONObject event = new JSONObject();
                event.put("id", cursor.getLong(0));
                event.put("title", shortText(cursor.getString(1), 60));
                event.put("begin", begin);
                event.put("end", finish);
                event.put("allDay", allDay);
                events.put(event);
            }
        } finally {
            cursor.close();
        }
        return events;
    }

    private static JSONObject scheduleAlert(SharedPreferences prefs, JSONArray events, long now)
            throws Exception {
        for (int index = 0; index < events.length(); index++) {
            JSONObject event = events.optJSONObject(index);
            if (event == null || event.optBoolean("allDay", false)) continue;
            long begin = event.optLong("begin", 0L);
            long remaining = begin - now;
            if (remaining <= 0L || remaining > 5L * 60L * 1000L) continue;
            String key = "v5|" + event.optLong("id", 0L) + "|" + begin;
            if (prefs.getLong(eventAlertPreferenceKey(key), 0L) > 0L) continue;
            int minutes = Math.max(1, (int) Math.ceil(remaining / 60000.0));
            String title = event.optString("title", "予定");
            JSONObject alert = alert("schedule", "予定", "あと" + minutes + "分で「"
                    + title + "」です。", true);
            alert.put("dedupKey", key);
            return alert;
        }
        return null;
    }

    private static JSONObject rainAlert(SharedPreferences prefs, JSONArray hourly, long now)
            throws Exception {
        long lastRainAlertAt = prefs.getLong(KEY_LAST_RAIN_ALERT + "_at", 0L);
        if (now - lastRainAlertAt < 4L * 60L * 60L * 1000L) return null;
        long horizon = now + 60L * 60L * 1000L;
        for (int index = 0; index < hourly.length(); index++) {
            JSONObject hour = hourly.optJSONObject(index);
            if (hour == null) continue;
            long time = hour.optLong("time", 0L);
            int probability = hour.optInt("probability", -1);
            double precipitation = hour.optDouble("precipitation", 0.0);
            int code = hour.optInt("code", -1);
            if (time < now - 10L * 60L * 1000L || time > horizon) continue;
            if (probability < 50 && precipitation < 0.1 && !isRainCode(code)) continue;
            String key = String.valueOf(time);
            if (key.equals(prefs.getString(KEY_LAST_RAIN_ALERT, ""))) return null;
            String place = prefs.getString(KEY_WEATHER_LOCATION, "現在地").trim();
            if (place.length() == 0) place = "現在地";
            String detail = probability >= 0 ? "（降水確率" + probability + "%）" : "";
            JSONObject alert = alert("rain", "雨の接近", "1時間以内に" + place
                    + "で雨の可能性があります" + detail + "。傘を確認してください。", true);
            alert.put("dedupKey", key);
            return alert;
        }
        return null;
    }

    private static JSONObject transitAlert(Context context, SharedPreferences prefs, long now)
            throws Exception {
        JSONObject maps = MailNotificationService.recentTransitJson();
        // Active Google Maps guidance is rendered by the dedicated navigation HUD.
        // Sending the same text as a proactive alert leaves a second copy in the
        // conversation area and competes with ordinary assistant messages.
        if (maps.optBoolean("navigationActive", false)) return null;
        String compact = maps.optBoolean("ok", false) ? maps.optString("compact", "").trim() : "";
        boolean speak = containsAny(compact, "まもなく", "降り", "乗り換", "乗換");
        if (compact.length() == 0) {
            long time = prefs.getLong("transit_gps_time", 0L);
            float speed = prefs.getFloat("transit_gps_speed", 0f);
            if (now - time <= TRANSIT_FRESH_MS && speed >= 3.5f) {
                compact = prefs.getString("transit_gps_compact", "").trim();
            }
        }
        if (compact.length() == 0) return null;
        String key = compact;
        if (key.equals(prefs.getString(KEY_LAST_TRANSIT_ALERT, ""))) return null;
        JSONObject alert = alert("transit", "移動案内", compact, speak);
        alert.put("dedupKey", key);
        return alert;
    }

    private static JSONObject buildOfflineCache(SharedPreferences prefs, JSONArray events,
                                                 JSONArray rain, long now) throws Exception {
        JSONObject cache = new JSONObject();
        cache.put("generatedAt", now);
        cache.put("events", events);
        cache.put("weatherLocation", prefs.getString(KEY_WEATHER_LOCATION, ""));
        cache.put("rain", rain);
        cache.put("transit", prefs.getString("transit_gps_compact", ""));
        return cache;
    }

    private static void rememberAlert(SharedPreferences prefs, JSONObject alert) {
        String type = alert.optString("type", "");
        String key = alert.optString("dedupKey", "");
        SharedPreferences.Editor editor = prefs.edit();
        if ("schedule".equals(type)) {
            editor.putString(KEY_LAST_EVENT_ALERT, key)
                    .putLong(eventAlertPreferenceKey(key), System.currentTimeMillis());
        } else if ("rain".equals(type)) {
            editor.putString(KEY_LAST_RAIN_ALERT, key)
                    .putLong(KEY_LAST_RAIN_ALERT + "_at", System.currentTimeMillis());
        }
        else if ("transit".equals(type)) editor.putString(KEY_LAST_TRANSIT_ALERT, key);
        editor.apply();
    }

    private static JSONObject alert(String type, String title, String message, boolean speak)
            throws Exception {
        JSONObject alert = new JSONObject();
        alert.put("type", type);
        alert.put("title", title);
        alert.put("message", message);
        alert.put("speak", speak);
        return alert;
    }

    private static String eventAlertPreferenceKey(String key) {
        return KEY_LAST_EVENT_ALERT + "_" + Integer.toHexString(
                key == null ? 0 : key.hashCode());
    }

    private static void refreshRainIfNeeded(Context context, SharedPreferences prefs, long now) {
        if (now - prefs.getLong(KEY_RAIN_UPDATED_AT, 0L) < RAIN_REFRESH_MS) return;
        try {
            Location location = bestLocation(context);
            if (location == null) return;
            String url = "https://api.open-meteo.com/v1/forecast?latitude="
                    + String.format(Locale.US, "%.5f", location.getLatitude())
                    + "&longitude=" + String.format(Locale.US, "%.5f", location.getLongitude())
                    + "&hourly=precipitation_probability,precipitation,weather_code"
                    + "&forecast_days=1&timeformat=unixtime&timezone=auto";
            JSONObject response = new JSONObject(fetch(url));
            JSONObject source = response.optJSONObject("hourly");
            if (source == null) return;
            JSONArray times = source.optJSONArray("time");
            JSONArray probabilities = source.optJSONArray("precipitation_probability");
            JSONArray precipitation = source.optJSONArray("precipitation");
            JSONArray codes = source.optJSONArray("weather_code");
            JSONArray result = new JSONArray();
            int count = times == null ? 0 : times.length();
            for (int index = 0; index < count; index++) {
                long time = times.optLong(index, 0L) * 1000L;
                if (time < now - 60L * 60L * 1000L || time > now + 3L * 60L * 60L * 1000L) {
                    continue;
                }
                JSONObject hour = new JSONObject();
                hour.put("time", time);
                hour.put("probability", probabilities == null ? -1 : probabilities.optInt(index, -1));
                hour.put("precipitation", precipitation == null ? 0.0 : precipitation.optDouble(index, 0.0));
                hour.put("code", codes == null ? -1 : codes.optInt(index, -1));
                result.put(hour);
            }
            prefs.edit().putString(KEY_RAIN_HOURLY, result.toString())
                    .putLong(KEY_RAIN_UPDATED_AT, now).apply();
        } catch (Exception error) {
            Log.w(TAG, "rain refresh failed; keeping cache", error);
        }
    }

    private static Location bestLocation(Context context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return null;
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) return null;
        Location best = null;
        try {
            List<String> providers = manager.getProviders(true);
            for (String provider : providers) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return best;
    }

    private static String fetch(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "DennoHishoLoki/0.9");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private static JSONArray readJsonArray(String value) {
        try {
            return new JSONArray(value == null ? "[]" : value);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static boolean isRainCode(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82)
                || (code >= 95 && code <= 99);
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null) return false;
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static String shortText(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        return safe.length() <= max ? safe : safe.substring(0, max) + "…";
    }
}
