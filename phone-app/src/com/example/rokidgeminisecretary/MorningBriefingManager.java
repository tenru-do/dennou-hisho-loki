package com.example.rokidgeminisecretary;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MorningBriefingManager {
    private static final String TAG = "LokiMorning";
    private static final String PREFS = "phone_secretary";
    private static final String KEY_DATE = "morning_briefing_date";
    private static final String KEY_TIME = "morning_briefing_time";
    private static final String KEY_SLOT = "topic_briefing_slot";
    private static final String KEY_NEWS_CACHE = "topic_scheduled_news_cache";
    private static final String KEY_ENABLED = "morning_briefing_enabled";
    private static final String KEY_FORMAT_VERSION = "morning_briefing_format_version";
    private static final String KEY_CUSTOM = "custom_instructions";
    private static final String KEY_PROFILE_ZODIAC = "morning_profile_zodiac";
    private static final String KEY_HOROSCOPE_ANNOUNCED_DATE = "topic_horoscope_announced_date";
    private static final int FORMAT_VERSION = 11;
    private static final String KEY_WEATHER_LOCATION = "weather_location";
    private static final String KEY_WEATHER_CONDITION = "weather_condition";
    private static final String KEY_WEATHER_TEMPERATURE = "weather_temperature";
    private static final String KEY_WEATHER_FORECAST = "weather_forecast";
    private static final String FILE_NAME = "loki_morning_today.json";
    private static final String SEEN_FILE_NAME = "loki_morning_seen.json";
    private static final int MAX_SEEN_ITEMS = 220;
    private static final long NEWS_SEEN_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L;
    private static final long EVENT_SEEN_RETENTION_MS = 90L * 24L * 60L * 60L * 1000L;
    private static final long TRIVIA_SEEN_RETENTION_MS = 14L * 24L * 60L * 60L * 1000L;
    private static final Object LOCK = new Object();
    private static volatile boolean building;
    private static final TopicPlaybackLoader<JSONObject> playbackLoader = new TopicPlaybackLoader<>();

    private MorningBriefingManager() {
    }

    static boolean isCollectionEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, true);
    }

    static void setCollectionEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) ensureFreshAsync(context, false);
    }

    static boolean shouldBuildToday(Context context) {
        if (!isCollectionEnabled(context)) return false;
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        // Catch up after a late app/service start instead of requiring the first 15 minutes.
        int slot = currentTopicSlot();
        if (slot < 0) slot=7;
        String currentSlotKey = topicSlotKey(today, slot);
        String savedSlotKey = preferences(context).getString(KEY_SLOT, "");
        int version = preferences(context).getInt(KEY_FORMAT_VERSION, 0);
        JSONObject cache=newsCache(context);
        boolean empty=cachedSection(cache,"topNews").length()==0;
        if(empty && cache.optLong("attemptedAt",0)>0 && version==FORMAT_VERSION
                && System.currentTimeMillis()-cache.optLong("attemptedAt",0)<60000L)return false;
        return !currentSlotKey.equals(savedSlotKey) || version != FORMAT_VERSION
                || empty && System.currentTimeMillis()-cache.optLong("attemptedAt",0)>60000L;
    }

    private static int currentTopicSlot() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 7) return -1;
        if (hour < 12) return 7;
        if (hour < 17) return 12;
        if (hour < 21) return 17;
        return 21;
    }

    private static String topicSlotKey(LocalDate date, int slot) {
        return date.toString() + "-" + slot;
    }

    private static String topicSlotLabel(int slot) {
        if (slot < 12) return "朝";
        if (slot < 17) return "昼";
        if (slot < 21) return "夕方";
        return "夜";
    }

    static synchronized void ensureFreshAsync(final Context context, final boolean force) {
        if (!isCollectionEnabled(context)
                || building || !force && !shouldBuildToday(context)) return;
        building = true;
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    collectScheduledNews(app);
                    Log.i(TAG, "scheduled topic news updated");
                } catch (Exception error) {
                    DailyContextSync.LOG.failed("TOPIC/briefing", error);
                } finally {
                    building = false;
                }
            }
        }, "LokiMorningBuild").start();
    }

    static JSONObject readStored(Context context) throws Exception {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            if (!isCollectionEnabled(context)) return statusJson("disabled");
            ensureFreshAsync(context, false);
            return statusJson("preparing");
        }
        String json = readAll(new FileInputStream(file));
        JSONObject stored = new JSONObject(json);
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        if (!today.equals(stored.optString("date", ""))) {
            boolean enabled = isCollectionEnabled(context);
            if (enabled) ensureFreshAsync(context, false);
            JSONObject stale = statusJson(enabled ? "preparing" : "disabled");
            stale.put("previousDate", stored.optString("date", ""));
            return stale;
        }
        stored.put("collectionEnabled", isCollectionEnabled(context));
        return stored;
    }

    static JSONObject readStoredForPlayback(Context context) throws Exception {
        JSONObject before=newsCache(context);
        ensureFreshAsync(context, cachedSection(before,"topNews").length()==0
                && System.currentTimeMillis()-before.optLong("attemptedAt",0)>=60000L);
        JSONObject stored = playbackLoader.loadLazy(() -> buildAndStore(context),
                () -> localBriefing(context), 3500L, DailyContextSync.LOG);
        if (stored.optBoolean("ok", false) && !stored.optBoolean("fallback",false)) {
            try {
                markStoredItemsAsAnnounced(context, stored);
            } catch (Exception error) {
                Log.w(TAG, "morning announcement history update failed", error);
            }
        }
        return TopicWirePayload.prepare(context, stored);
    }

    static JSONObject newsCache(Context context) {
        try {
            JSONObject cache=new JSONObject(preferences(context).getString(KEY_NEWS_CACHE,"{}"));
            return LocalDate.now().toString().equals(cache.optString("date"))?cache:new JSONObject();
        }catch(Exception e){return new JSONObject();}
    }
    static boolean initialNewsPending(Context context) {
        JSONObject cache=newsCache(context);
        return building && cachedSection(cache,"topNews").length()==0
                && System.currentTimeMillis()-cache.optLong("attemptedAt",0)<6000L;
    }

    private static void collectScheduledNews(Context context) throws Exception {
        SharedPreferences prefs = preferences(context);
        LocalDate date = LocalDate.now(ZoneId.systemDefault());
        int slot = currentTopicSlot();
        String region = clean(prefs.getString(KEY_WEATHER_LOCATION, ""));
        if (region.length() == 0 || "取得済".equals(region)) region = "立川・多摩地域";
        JSONObject cache = newsCache(context);
        cache.put("date", date.toString());
        cache.put("attemptedAt", System.currentTimeMillis());
        prefs.edit().putString(KEY_NEWS_CACHE,cache.toString()).apply();
        final String newsRegion = TopicTextPolicy.region(region);
        JSONArray top=ContextSyncPolicy.attempt("TOPIC/news",
                () -> fetchNews("", 10), new JSONArray(), DailyContextSync.LOG);
        if(top.length()>0){cache.put("topNews",top);cache.put("collectedAt",System.currentTimeMillis());}
        // Publish the main headlines immediately; local search/POI must not delay them.
        prefs.edit().putString(KEY_NEWS_CACHE, cache.toString()).apply();
        cache.put("localNews", ContextSyncPolicy.attempt("TOPIC/localNews",
                () -> fetchNews(newsRegion + " ニュース", 7), new JSONArray(), DailyContextSync.LOG));
        cache.put("localEvents", ContextSyncPolicy.attempt("TOPIC/events",
                () -> fetchNews(newsRegion + " (イベント OR 祭り OR 展示 OR 催し) when:30d", 40), new JSONArray(), DailyContextSync.LOG));
        JSONArray raw=cachedSection(cache,"localEvents");
        JSONArray upcoming=filterUpcomingEvents(raw,date,30,7);
        Log.i("LokiTopicData","TOPIC/events api=GoogleNewsRSS cachedCandidateCount="+raw.length()
                +" selectedCount="+upcoming.length()+" daysAhead=30 queryRegion="+newsRegion);
        cache.put("upcomingEvents",upcoming);
        prefs.edit().putString(KEY_NEWS_CACHE,cache.toString()).apply();
        if(upcoming.length()==0)NearbyTopicPois.refresh(context);
        prefs.edit().putString(KEY_NEWS_CACHE, cache.toString())
                .putString(KEY_SLOT, top.length()>0?topicSlotKey(date, slot):"")
                .putInt(KEY_FORMAT_VERSION, FORMAT_VERSION).apply();
    }

    private static JSONArray cachedSection(JSONObject cache, String name) {
        JSONArray items = cache.optJSONArray(name);
        return items == null ? new JSONArray() : items;
    }

    private static void markStoredItemsAsAnnounced(Context context, JSONObject stored)
            throws Exception {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            LocalDate date = LocalDate.now(ZoneId.systemDefault());
            JSONArray seen = readSeenItems(context, now);
            rememberAnnouncedSection(stored.optJSONArray("topNews"), "news",
                    date, now, seen);
            rememberAnnouncedSection(stored.optJSONArray("localNews"), "news",
                    date, now, seen);
            rememberAnnouncedSection(stored.optJSONArray("localEvents"), "event",
                    date, now, seen);
            rememberAnnouncedSection(stored.optJSONArray("mails"), "mail",
                    date, now, seen);
            rememberAnnouncedSection(stored.optJSONArray("trivia"), "trivia",
                    date, now, seen);
            writeSeenAtomically(context, seen);
            JSONArray horoscope = stored.optJSONArray("horoscope");
            if (horoscope != null && horoscope.length() > 0) {
                preferences(context).edit()
                        .putString(KEY_HOROSCOPE_ANNOUNCED_DATE, date.toString()).apply();
            }
        }
    }

    private static void rememberAnnouncedSection(JSONArray source, String kind,
                                                  LocalDate date, long now,
                                                  JSONArray seen) throws Exception {
        if (source == null) return;
        for (int index = 0; index < source.length(); index++) {
            JSONObject sourceItem = source.optJSONObject(index);
            if (sourceItem == null) continue;
            String normalized = normalizeHeadline(sourceItem.optString("title", ""));
            if (normalized.length() < 5) continue;
            JSONObject item = findSimilarSeenItem(seen, kind, normalized);
            if (item == null) {
                item = new JSONObject();
                item.put("kind", kind);
                seen.put(item);
            }
            item.put("normalized", shortText(normalized, 180));
            item.put("lastSeenAt", now);
            item.put("announcedAt", now);
            if ("event".equals(kind) && date.getDayOfWeek().getValue() >= 5) {
                item.put("weekendKey", eventRepeatKey(date));
            }
        }
    }

    private static JSONObject localBriefing(Context context) throws Exception {
        JSONObject topic = TopicFallbackContent.local(context);
        String script = topic.getString("text");
        return new JSONObject().put("ok", true).put("status", "ready").put("fallback", true)
                .put("date", LocalDate.now(ZoneId.systemDefault()).toString())
                .put("script", script).put("summary", script).put("title", topic.getString("title"))
                .put("trivia", new JSONArray().put(topic)).put("generatedAt", System.currentTimeMillis());
    }
    private static JSONObject statusJson(String status) throws Exception {
        JSONObject root = new JSONObject();
        root.put("ok", false);
        root.put("status", status);
        root.put("building", building);
        root.put("collectionEnabled", !"disabled".equals(status));
        return root;
    }

    private static JSONObject buildAndStore(Context context) throws Exception {
        synchronized (LOCK) {
            LocalDate date = LocalDate.now(ZoneId.systemDefault());
            long generatedAt = System.currentTimeMillis();
            int slot = currentTopicSlot();
            if (slot < 0) slot = 7;
            String slotLabel = topicSlotLabel(slot);
            SharedPreferences prefs = preferences(context);
            JSONArray events = ContextSyncPolicy.attempt("TOPIC/calendar",
                    () -> readTodayEvents(context), new JSONArray(), DailyContextSync.LOG);
            JSONArray rawMails = recentMailsSince(
                    generatedAt - 7L * 24L * 60L * 60L * 1000L);
            JSONObject dedupedMails = deduplicateMailItems(
                    context, date, generatedAt, rawMails);
            JSONArray mails = dedupedMails.optJSONArray("mails");
            JSONArray skippedMails = dedupedMails.optJSONArray("skippedMails");
            JSONObject gmailUnread = ContextSyncPolicy.attempt("TOPIC/mail",
                    () -> GmailUnreadReader.read(context), new JSONObject(), DailyContextSync.LOG);
            Log.i(TAG, "mail candidates raw=" + rawMails.length()
                    + " fresh=" + (mails == null ? 0 : mails.length())
                    + " skipped=" + (skippedMails == null ? 0 : skippedMails.length())
                    + " gmailUnread=" + gmailUnread.optInt("unreadInbox", -1)
                    + " gmailStatus=" + gmailUnread.optString("status", "unknown"));
            String region = clean(prefs.getString(KEY_WEATHER_LOCATION, ""));
            if (region.length() == 0 || "取得済".equals(region)) region = "立川・多摩地域";

            HourlyForecast homeHourly = null;
            Location home = ContextSyncPolicy.attempt("LOCATION/briefing",
                    () -> bestLastLocation(context), null, DailyContextSync.LOG);
            if (home != null) {
                try {
                    homeHourly = fetchHourly(home.getLatitude(), home.getLongitude());
                } catch (Exception error) {
                    DailyContextSync.LOG.failed("WEATHER/briefing", error);
                }
            }

            JSONArray eventWeather = buildEventWeather(context, events, homeHourly, region);
            JSONObject newsCache = newsCache(context);
            // Do not pass yesterday's headlines off as today's updates.
            if (!date.toString().equals(newsCache.optString("date", ""))) newsCache = new JSONObject();
            JSONArray rawTopNews = cachedSection(newsCache, "topNews");
            JSONArray rawLocalNews = cachedSection(newsCache, "localNews");
            JSONArray rawLocalEvents = filterUpcomingEvents(
                    cachedSection(newsCache, "localEvents"),
                    date, 30, 7);
            JSONObject deduped = deduplicateBriefingItems(context, date, generatedAt,
                    rawTopNews, rawLocalNews, rawLocalEvents);
            JSONArray topNews = deduped.optJSONArray("topNews");
            JSONArray localNews = deduped.optJSONArray("localNews");
            JSONArray localEvents = deduped.optJSONArray("localEvents");
            JSONArray skippedTopNews = deduped.optJSONArray("skippedTopNews");
            JSONArray skippedLocalNews = deduped.optJSONArray("skippedLocalNews");
            JSONArray skippedLocalEvents = deduped.optJSONArray("skippedLocalEvents");
            JSONArray horoscope = date.toString().equals(
                    prefs.getString(KEY_HOROSCOPE_ANNOUNCED_DATE, ""))
                    ? new JSONArray() : buildHoroscope(date, prefs);
            JSONArray trivia = buildTimelyTrivia(context, date, slot,
                    topNews, localNews, localEvents);
            String script = buildScript(date, slotLabel, region, prefs, events, mails,
                    gmailUnread,
                    homeHourly, eventWeather, topNews, localNews, localEvents,
                    horoscope, trivia);
            String summary = buildSummary(date, region, events, homeHourly,
                    topNews, localNews, horoscope, skippedTopNews,
                    skippedLocalNews, skippedLocalEvents, trivia);

            JSONObject root = new JSONObject();
            root.put("ok", true);
            root.put("status", "ready");
            root.put("collectionEnabled", true);
            root.put("formatVersion", FORMAT_VERSION);
            root.put("title", "ロキ・トピック");
            root.put("date", date.toString());
            root.put("slot", slot);
            root.put("slotLabel", slotLabel);
            root.put("generatedAt", generatedAt);
            root.put("personalDataRefreshedAt", generatedAt);
            root.put("newsCollectedAt", newsCache.optLong("collectedAt", 0L));
            root.put("region", region);
            root.put("script", script);
            root.put("summary", summary);
            root.put("characterCount", script.length());
            root.put("estimatedMinutes", Math.max(1,
                    Math.round((script.length() * 0.185f) / 60.0f)));
            root.put("events", events);
            root.put("mails", mails);
            root.put("skippedMails", skippedMails);
            root.put("gmailUnread", gmailUnread);
            root.put("eventWeather", eventWeather);
            root.put("topNews", topNews);
            root.put("localNews", localNews);
            root.put("localEvents", localEvents);
            root.put("skippedTopNews", skippedTopNews);
            root.put("skippedLocalNews", skippedLocalNews);
            root.put("skippedLocalEvents", skippedLocalEvents);
            root.put("horoscope", horoscope);
            root.put("trivia", trivia);
            writeAtomically(context, root.toString());
            prefs.edit().putString(KEY_DATE, date.toString())
                    .putLong(KEY_TIME, generatedAt)
                    .apply();
            MainActivity.rememberMorningBriefing(context, summary);
            return root;
        }
    }

    private static JSONArray readTodayEvents(Context context) throws Exception {
        JSONArray result = new JSONArray();
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) return result;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        long now = System.currentTimeMillis();
        long start = today.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        android.content.ContentUris.appendId(builder, start);
        android.content.ContentUris.appendId(builder, end - 1L);
        String[] projection = new String[]{
                CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION};
        Cursor cursor = context.getContentResolver().query(builder.build(), projection,
                CalendarContract.Instances.VISIBLE + "!=0", null,
                CalendarContract.Instances.BEGIN + " ASC");
        if (cursor == null) return result;
        int droppedOutsideToday = 0;
        try {
            while (cursor.moveToNext() && result.length() < 16) {
                long begin = cursor.getLong(1);
                long eventEnd = cursor.getLong(2);
                boolean allDay = cursor.getInt(3) != 0;
                if (allDay) {
                    // All-day boundaries are UTC dates in CalendarContract. Treating them as
                    // local instants makes yesterday's item overlap the start of today in Japan.
                    LocalDate allDayStart = Instant.ofEpochMilli(begin)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate();
                    LocalDate allDayEndExclusive = Instant.ofEpochMilli(eventEnd)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate();
                    if (today.isBefore(allDayStart) || !today.isBefore(allDayEndExclusive)) {
                        droppedOutsideToday++;
                        continue;
                    }
                } else if (eventEnd <= now) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("title", shortText(cursor.getString(0), 70));
                item.put("begin", begin);
                item.put("end", eventEnd);
                item.put("allDay", allDay);
                item.put("location", shortText(cursor.getString(4), 100));
                result.put(item);
            }
        } finally {
            cursor.close();
        }
        Log.i(TAG, "calendar remaining count=" + result.length()
                + " droppedOutsideTodayAllDay=" + droppedOutsideToday);
        return result;
    }

    private static JSONArray recentMailsSince(long since) throws Exception {
        JSONArray source = MailNotificationService.recentMailJson();
        JSONArray result = new JSONArray();
        for (int i = 0; i < source.length() && result.length() < 8; i++) {
            JSONObject mail = source.optJSONObject(i);
            if (mail != null && mail.optLong("time", 0L) >= since) result.put(mail);
        }
        return result;
    }

    private static JSONArray buildEventWeather(Context context, JSONArray events,
                                                HourlyForecast fallback,
                                                String homeRegion) throws Exception {
        JSONArray result = new JSONArray();
        HashMap<String, HourlyForecast> cache = new HashMap<String, HourlyForecast>();
        HashMap<String, String> regionCache = new HashMap<String, String>();
        ArrayList<String> announcedPlaces = new ArrayList<String>();
        ArrayList<Long> announcedTimes = new ArrayList<Long>();
        int destinations = 0;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null || event.optBoolean("allDay", false)) continue;
            String place = clean(event.optString("location", ""));
            if (place.length() == 0) continue;
            long begin = event.optLong("begin", 0L);
            String normalizedPlace = normalizePlace(place);
            boolean duplicate = false;
            for (int previous = 0; previous < announcedPlaces.size(); previous++) {
                if (normalizedPlace.equals(announcedPlaces.get(previous))
                        && Math.abs(begin - announcedTimes.get(previous))
                        <= 4L * 60L * 60L * 1000L) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;
            announcedPlaces.add(normalizedPlace);
            announcedTimes.add(begin);
            HourlyForecast forecast = fallback;
            boolean destinationSpecific = false;
            String weatherRegion = clean(homeRegion);
            if (place.length() > 0 && destinations < 2) {
                String key = normalizePlace(place);
                if (cache.containsKey(key)) {
                    forecast = cache.get(key);
                    destinationSpecific = forecast != null;
                    weatherRegion = destinationSpecific
                            ? clean(regionCache.get(key)) : clean(homeRegion);
                } else {
                    destinations++;
                    try {
                        GeocodedPlace coordinate = geocode(context, place);
                        forecast = coordinate == null ? fallback
                                : fetchHourly(coordinate.latitude, coordinate.longitude);
                        cache.put(key, forecast);
                        destinationSpecific = coordinate != null && forecast != null;
                        weatherRegion = destinationSpecific
                                ? coordinate.regionLabel : clean(homeRegion);
                        regionCache.put(key, weatherRegion);
                    } catch (Exception error) {
                        cache.put(key, null);
                        regionCache.put(key, clean(homeRegion));
                        forecast = fallback;
                        weatherRegion = clean(homeRegion);
                    }
                }
            }
            if (weatherRegion.length() == 0) weatherRegion = "現在地周辺";
            JSONObject weather = rainAdvice(forecast,
                    begin - 90L * 60L * 1000L,
                    event.optLong("end", 0L) + 90L * 60L * 1000L,
                    weatherRegion);
            weather.put("title", event.optString("title", ""));
            weather.put("begin", begin);
            weather.put("location", place);
            weather.put("weatherRegion", weatherRegion);
            weather.put("destinationSpecific", destinationSpecific);
            result.put(weather);
        }
        return result;
    }

    private static GeocodedPlace geocode(Context context, String place) throws Exception {
        if (!Geocoder.isPresent()) return null;
        List<Address> addresses = new Geocoder(context, Locale.JAPAN)
                .getFromLocationName(place, 1);
        if (addresses == null || addresses.isEmpty()) return null;
        Address address = addresses.get(0);
        String admin = clean(address.getAdminArea());
        String locality = clean(address.getLocality());
        if (locality.length() == 0) locality = clean(address.getSubAdminArea());
        String label = locality;
        if (admin.length() > 0 && !label.startsWith(admin)) label = admin + label;
        if (label.length() == 0) label = "予定先周辺";
        return new GeocodedPlace(address.getLatitude(), address.getLongitude(),
                shortText(label, 40));
    }

    private static Location bestLastLocation(Context context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return null;
        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            if (manager != null) {
                for (String provider : manager.getProviders(true)) {
                    Location candidate = manager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                        best = candidate;
                    }
                }
            }
            return best;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static HourlyForecast fetchHourly(double latitude, double longitude) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast?latitude="
                + String.format(Locale.US, "%.5f", latitude)
                + "&longitude=" + String.format(Locale.US, "%.5f", longitude)
                + "&hourly=temperature_2m,precipitation_probability,weather_code"
                + "&forecast_days=2&timezone=auto";
        JSONObject root = new JSONObject(fetchText(url));
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly == null) return null;
        return new HourlyForecast(hourly.optJSONArray("time"),
                hourly.optJSONArray("precipitation_probability"),
                hourly.optJSONArray("temperature_2m"));
    }

    private static JSONObject rainAdvice(HourlyForecast forecast, long start, long end,
                                         String place) throws Exception {
        JSONObject result = new JSONObject();
        result.put("ok", forecast != null);
        result.put("place", shortText(place, 80));
        if (forecast == null || forecast.times == null) {
            result.put("text", "時間帯別の雨予報を取得できませんでした");
            return result;
        }
        int max = -1;
        long firstRain = 0L;
        double low = Double.NaN;
        double high = Double.NaN;
        ZoneId zone = ZoneId.systemDefault();
        for (int i = 0; i < forecast.times.length(); i++) {
            String time = forecast.times.optString(i, "");
            if (time.length() == 0) continue;
            long millis;
            try {
                millis = LocalDateTime.parse(time).atZone(zone).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                continue;
            }
            if (millis < start || millis > end) continue;
            int probability = forecast.rain == null ? -1 : forecast.rain.optInt(i, -1);
            if (probability > max) max = probability;
            if (firstRain == 0L && probability >= 30) firstRain = millis;
            if (forecast.temperatures != null && !forecast.temperatures.isNull(i)) {
                double value = forecast.temperatures.optDouble(i, Double.NaN);
                if (!Double.isNaN(value)) {
                    low = Double.isNaN(low) ? value : Math.min(low, value);
                    high = Double.isNaN(high) ? value : Math.max(high, value);
                }
            }
        }
        String umbrella;
        if (max >= 70) umbrella = "長傘が安心です";
        else if (max >= 40) umbrella = "折りたたみ傘を持ってください";
        else if (max >= 20) umbrella = "念のため折りたたみ傘があると安心です";
        else umbrella = "雨具は基本的に不要そうです";
        String first = firstRain == 0L ? ""
                : new java.text.SimpleDateFormat("H時頃", Locale.JAPAN).format(new Date(firstRain));
        String text = (first.length() == 0 ? "目立った雨の開始は見込まれていません"
                : first + "から雨の可能性があります")
                + "。最大降水確率は" + Math.max(0, max) + "パーセントで、" + umbrella;
        if (!Double.isNaN(low) && !Double.isNaN(high)) {
            text += "。移動時間帯の気温はおよそ"
                    + Math.round(low) + "度から" + Math.round(high) + "度です";
        }
        result.put("maxRain", Math.max(0, max));
        result.put("firstRainAt", firstRain);
        result.put("umbrella", umbrella);
        result.put("text", text + "。");
        return result;
    }

    static JSONArray publicNewsFallback(int max) throws Exception {
        return newsArticles(null, max);
    }
    private static JSONArray fetchNews(String query, int max) throws Exception {
        String primary = query.isEmpty()
                ? "https://news.google.com/rss?hl=ja&gl=JP&ceid=JP:ja"
                : "https://news.google.com/rss/search?q=" + URLEncoder.encode(query, "UTF-8")
                    + "&hl=ja&gl=JP&ceid=JP:ja";
        // Regional/event feeds must not be replaced by unrelated national headlines.
        if (!query.isEmpty()) {
            JSONArray result = new JSONArray();
            try {
                List<NewsRssFallback.Article> parsed=NewsRssFallback.parse(fetchText(primary), Integer.MAX_VALUE);
                int chars=0;
                for (NewsRssFallback.Article article : parsed) {
                    chars+=article.title.length()+article.description.length();
                    if(result.length()<max)result.put(bindNewsArticle(article));
                }
                Log.i("LokiTopicData","TOPIC/rss source=GoogleNewsSearch queryType="+(query.contains("(イベント")?"events":"localNews")
                        +" rawResponseCount="+parsed.size()+" parsedChars="+chars+" selectedCount="+result.length());
            }
            catch (Exception error) { DailyContextSync.LOG.failed("TOPIC/regional-news", error); }
            return result;
        }
        return newsArticles(primary, max);
    }
    private static JSONArray newsArticles(String primary, int max) throws Exception {
        JSONArray result = new JSONArray();
        for (NewsRssFallback.Article article : NewsRssFallback.fetch(primary, max, url -> {
            try { return fetchText(url); }
            catch (Exception error) { DailyContextSync.LOG.failed("TOPIC/rss", error); throw error; }
        },(url,count,chars,status)->Log.i("LokiTopicData","TOPIC/rss source="+url
                +" rawResponseCount="+count+" parsedChars="+chars+" status="+status))) result.put(bindNewsArticle(article));
        Log.i(TAG, "TOPIC RSS articles=" + result.length());
        return result;
    }
    private static JSONObject bindNewsArticle(NewsRssFallback.Article article) throws Exception {
        return new JSONObject().put("title", shortText(article.title, 110))
                .put("source", shortText(article.source, 40)).put("published", article.published)
                .put("link", article.link).put("description", shortText(NewsSpeechText.detail(article.title, article.description, article.source), 500));
    }
    private static JSONArray filterUpcomingEvents(JSONArray source, LocalDate today,
                                                   int daysAhead, int max) throws Exception {
        JSONArray result = new JSONArray();
        LocalDate limit = today.plusDays(Math.max(0, daysAhead));
        for (int index = 0; index < source.length() && result.length() < max; index++) {
            JSONObject item = source.optJSONObject(index);
            if (item == null) continue;
            String text = item.optString("title", "") + " "
                    + item.optString("description", "");
            ArrayList<LocalDate> dates = extractEventDates(text, today,
                    item.optString("published", ""));
            LocalDate selected = null;
            for (LocalDate candidate : dates) {
                if (!candidate.isBefore(today) && !candidate.isAfter(limit)
                        && (selected == null || candidate.isBefore(selected))) {
                    selected = candidate;
                }
            }
            if (selected == null) continue;
            item.put("eventDate", selected.toString());
            item.put("eventDateLabel", DateTimeFormatter.ofPattern(
                    "M月d日、EEEE", Locale.JAPAN).format(selected));
            result.put(item);
        }
        return result;
    }

    private static ArrayList<LocalDate> extractEventDates(String text, LocalDate today,
                                                          String published) {
        ArrayList<LocalDate> result = new ArrayList<LocalDate>();
        String value = clean(text);
        Matcher japanese = Pattern.compile(
                "(?:(20\\d{2})年)?\\s*(\\d{1,2})月\\s*(\\d{1,2})日")
                .matcher(value);
        while (japanese.find()) {
            addEventDate(result, today, japanese.group(1),
                    japanese.group(2), japanese.group(3));
        }
        Matcher slash = Pattern.compile(
                "(?<!\\d)(?:(20\\d{2})[./-])?(\\d{1,2})[./-](\\d{1,2})(?!\\d)")
                .matcher(value);
        while (slash.find()) {
            addEventDate(result, today, slash.group(1), slash.group(2), slash.group(3));
        }
        boolean recentArticle = isRecentPublishedDate(published, today, 10);
        if (recentArticle) {
            if (value.contains("明後日")) result.add(today.plusDays(2));
            if (value.contains("明日")) result.add(today.plusDays(1));
            if (value.contains("今日") || value.contains("本日")) result.add(today);
            if (value.contains("今週末")) {
                int untilSaturday = (6 - today.getDayOfWeek().getValue() + 7) % 7;
                result.add(today.plusDays(untilSaturday));
            }
        }
        return result;
    }

    private static void addEventDate(ArrayList<LocalDate> target, LocalDate today,
                                     String yearText, String monthText, String dayText) {
        try {
            int year = yearText == null || yearText.length() == 0
                    ? today.getYear() : Integer.parseInt(yearText);
            int month = Integer.parseInt(monthText);
            int day = Integer.parseInt(dayText);
            LocalDate candidate = LocalDate.of(year, month, day);
            if ((yearText == null || yearText.length() == 0)
                    && candidate.isBefore(today.minusDays(1))) {
                candidate = candidate.plusYears(1);
            }
            if (!target.contains(candidate)) target.add(candidate);
        } catch (Exception ignored) {
        }
    }

    private static boolean isRecentPublishedDate(String published, LocalDate today, int days) {
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date parsed = format.parse(published);
            if (parsed == null) return false;
            LocalDate date = parsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return !date.isAfter(today.plusDays(1)) && !date.isBefore(today.minusDays(days));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String stripHtml(String value) {
        return clean(value.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replaceAll("\\s+", " "));
    }

    private static JSONObject deduplicateBriefingItems(Context context, LocalDate date,
                                                       long now, JSONArray topNews,
                                                       JSONArray localNews,
                                                       JSONArray localEvents) throws Exception {
        JSONArray seen = readSeenItems(context, now);
        JSONObject result = new JSONObject();
        JSONArray spokenTop = new JSONArray();
        JSONArray skippedTop = new JSONArray();
        filterBriefingSection(topNews, "news", date, now, seen, spokenTop, skippedTop);
        JSONArray spokenLocal = new JSONArray();
        JSONArray skippedLocal = new JSONArray();
        filterBriefingSection(localNews, "news", date, now, seen, spokenLocal, skippedLocal);
        JSONArray spokenEvents = new JSONArray();
        JSONArray skippedEvents = new JSONArray();
        filterBriefingSection(localEvents, "event", date, now, seen,
                spokenEvents, skippedEvents);
        result.put("topNews", spokenTop);
        result.put("localNews", spokenLocal);
        result.put("localEvents", spokenEvents);
        result.put("skippedTopNews", skippedTop);
        result.put("skippedLocalNews", skippedLocal);
        result.put("skippedLocalEvents", skippedEvents);
        return result;
    }

    private static JSONObject deduplicateMailItems(Context context, LocalDate date,
                                                   long now, JSONArray source)
            throws Exception {
        JSONArray spoken = new JSONArray();
        JSONArray skipped = new JSONArray();
        JSONArray seen = readSeenItems(context, now);
        filterBriefingSection(source, "mail", date, now, seen, spoken, skipped);
        JSONObject result = new JSONObject();
        result.put("mails", spoken);
        result.put("skippedMails", skipped);
        return result;
    }

    private static void filterBriefingSection(JSONArray source, String kind,
                                               LocalDate date, long now, JSONArray seen,
                                               JSONArray spoken, JSONArray skipped)
            throws Exception {
        if (source == null) return;
        boolean weekendWindow = "event".equals(kind)
                && date.getDayOfWeek().getValue() >= 5;
        String weekendKey = eventRepeatKey(date);
        for (int index = 0; index < source.length(); index++) {
            JSONObject item = source.optJSONObject(index);
            if (item == null) continue;
            String normalized = normalizeHeadline(item.optString("title", ""));
            if (normalized.length() < 5) continue;
            JSONObject matched = findSimilarSeenItem(seen, kind, normalized);
            boolean announce = matched == null;
            if(!announce&&"news".equals(kind)&&NewsSpeechText.update(matched.optString("normalized"),normalized)){announce=true;item=new JSONObject(item.toString()).put("title","続報、"+item.optString("title"));}
            if (!announce && weekendWindow
                    && !weekendKey.equals(matched.optString("weekendKey", ""))) {
                announce = true;
            }
            if (matched == null) {
                matched = new JSONObject();
                matched.put("kind", kind);
                seen.put(matched);
            }
            matched.put("normalized", shortText(normalized, 180));
            matched.put("lastSeenAt", now);
            if (announce) {
                matched.put("announcedAt", now);
                if (weekendWindow) matched.put("weekendKey", weekendKey);
                spoken.put(item);
            } else {
                skipped.put(item);
            }
        }
    }

    private static String eventRepeatKey(LocalDate date) {
        String week = date.minusDays(date.getDayOfWeek().getValue() - 1L).toString();
        int day = date.getDayOfWeek().getValue();
        if (day == 5) return week + "-friday";
        if (day >= 6) return week + "-weekend";
        return "";
    }

    private static JSONObject findSimilarSeenItem(JSONArray seen, String kind,
                                                  String normalized) {
        for (int index = 0; index < seen.length(); index++) {
            JSONObject item = seen.optJSONObject(index);
            if (item == null || !kind.equals(item.optString("kind", ""))) continue;
            if (similarHeadline(normalized, item.optString("normalized", ""))) {
                return item;
            }
        }
        return null;
    }

    private static boolean similarHeadline(String left, String right) {
        if (left.equals(right)) return true;
        if (Math.min(left.length(), right.length()) >= 14
                && (left.contains(right) || right.contains(left))) return true;
        if (Math.min(left.length(), right.length()) < 18) return false;
        Set<String> leftGrams = trigrams(left);
        Set<String> rightGrams = trigrams(right);
        if (leftGrams.isEmpty() || rightGrams.isEmpty()) return false;
        int common = 0;
        for (String gram : leftGrams) if (rightGrams.contains(gram)) common++;
        double dice = (2.0d * common) / (leftGrams.size() + rightGrams.size());
        return dice >= 0.72d;
    }

    private static Set<String> trigrams(String value) {
        HashSet<String> result = new HashSet<String>();
        for (int index = 0; index + 3 <= value.length(); index++) {
            result.add(value.substring(index, index + 3));
        }
        return result;
    }

    private static String normalizeHeadline(String title) {
        return clean(title).toLowerCase(Locale.JAPAN)
                .replace("速報", "").replace("続報", "")
                .replaceAll("[\\p{P}\\p{S}\\s　]+", "");
    }

    private static JSONArray buildTimelyTrivia(Context context, LocalDate date, int slot,
                                               JSONArray topNews, JSONArray localNews,
                                               JSONArray localEvents) throws Exception {
        JSONArray result = new JSONArray();
        int contentCount = (topNews == null ? 0 : topNews.length())
                + (localNews == null ? 0 : localNews.length())
                + (localEvents == null ? 0 : localEvents.length());
        if (contentCount >= 6) return result;

        result.put(TopicFallbackContent.local(context));
        return result;
    }
    private static void appendTitles(StringBuilder target, JSONArray source) {
        if (source == null) return;
        for (int index = 0; index < source.length(); index++) {
            JSONObject item = source.optJSONObject(index);
            if (item != null) target.append(' ').append(item.optString("title", ""));
        }
    }

    private static boolean containsAnyKeyword(String value, String... keywords) {
        if (value == null) return false;
        for (String keyword : keywords) if (value.contains(keyword)) return true;
        return false;
    }

    private static JSONArray readSeenItems(Context context, long now) {
        JSONArray result = new JSONArray();
        File file = new File(context.getFilesDir(), SEEN_FILE_NAME);
        if (!file.exists()) return result;
        try {
            JSONArray stored = new JSONArray(readAll(new FileInputStream(file)));
            for (int index = 0; index < stored.length(); index++) {
                JSONObject item = stored.optJSONObject(index);
                if (item == null) continue;
                String kind = item.optString("kind", "");
                long retention = "event".equals(kind) ? EVENT_SEEN_RETENTION_MS
                        : "trivia".equals(kind) ? TRIVIA_SEEN_RETENTION_MS
                        : NEWS_SEEN_RETENTION_MS;
                if (now - item.optLong("lastSeenAt", 0L) <= retention) result.put(item);
            }
        } catch (Exception error) {
            Log.w(TAG, "morning dedupe history read failed", error);
        }
        return result;
    }

    private static void writeSeenAtomically(Context context, JSONArray seen) throws Exception {
        if (seen == null) return;
        ArrayList<JSONObject> sorted = new ArrayList<JSONObject>();
        for (int index = 0; index < seen.length(); index++) {
            JSONObject item = seen.optJSONObject(index);
            if (item != null) sorted.add(item);
        }
        Collections.sort(sorted, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right) {
                return Long.compare(right.optLong("lastSeenAt", 0L),
                        left.optLong("lastSeenAt", 0L));
            }
        });
        JSONArray compact = new JSONArray();
        for (int index = 0; index < sorted.size() && index < MAX_SEEN_ITEMS; index++) {
            compact.put(sorted.get(index));
        }
        File target = new File(context.getFilesDir(), SEEN_FILE_NAME);
        File temporary = new File(context.getFilesDir(), SEEN_FILE_NAME + ".tmp");
        FileOutputStream output = new FileOutputStream(temporary, false);
        try {
            output.write(compact.toString().getBytes(StandardCharsets.UTF_8));
            output.flush();
        } finally {
            output.close();
        }
        if (target.exists() && !target.delete()) {
            throw new IllegalStateException("old morning dedupe history could not be replaced");
        }
        if (!temporary.renameTo(target)) {
            throw new IllegalStateException("morning dedupe history rename failed");
        }
    }

    private static JSONArray buildHoroscope(LocalDate date, SharedPreferences prefs) throws Exception {
        String[] zodiac = new String[]{"牡羊座", "牡牛座", "双子座", "蟹座", "獅子座", "乙女座",
                "天秤座", "蠍座", "射手座", "山羊座", "水瓶座", "魚座"};
        String[] fortunes = new String[]{
                "早めの一歩が流れを作る日", "丁寧な確認が幸運を呼ぶ日",
                "思いがけない連絡にヒントがある日", "身近な人との会話が力になる日",
                "小さな予定変更が好転につながる日", "整理整頓で集中力が上がる日",
                "迷ったら落ち着く方を選ぶとよい日", "休憩を挟むと判断が冴える日",
                "新しい情報を一つ試すとよい日", "無理をせず順番を守ると整う日",
                "いつもと違う道に発見がある日", "睡眠と水分補給を意識したい日"};
        ArrayList<String> order = new ArrayList<String>();
        Collections.addAll(order, zodiac);
        Collections.shuffle(order, new Random(date.toString().hashCode()));
        JSONArray result = new JSONArray();
        String profileSign = detectProfileZodiac(prefs.getString(KEY_CUSTOM, ""), zodiac);
        boolean detectedFromProfile = profileSign.length() > 0;
        if (profileSign.length() > 0) {
            prefs.edit().putString(KEY_PROFILE_ZODIAC, profileSign).apply();
        } else {
            String cachedSign = prefs.getString(KEY_PROFILE_ZODIAC, "");
            if (isKnownZodiac(cachedSign, zodiac)) profileSign = cachedSign;
        }
        Log.i(TAG, "horoscope profileDetected=" + detectedFromProfile
                + " available=" + (profileSign.length() > 0));
        if (profileSign.length() == 0) {
            JSONObject item = new JSONObject();
            item.put("rank", 0);
            item.put("sign", "全星座共通");
            item.put("fortune", fortunes[Math.floorMod(date.toString().hashCode(),
                    fortunes.length)]);
            item.put("generic", true);
            result.put(item);
            return result;
        }
        for (int i = 0; i < order.size(); i++) {
            if (!profileSign.equals(order.get(i))) continue;
            JSONObject item = new JSONObject();
            item.put("rank", i + 1);
            item.put("sign", order.get(i));
            item.put("fortune", fortunes[Math.floorMod(date.toString().hashCode() + i * 7,
                    fortunes.length)]);
            result.put(item);
        }
        return result;
    }

    private static boolean isKnownZodiac(String value, String[] zodiac) {
        if (value == null || value.length() == 0) return false;
        for (String sign : zodiac) if (sign.equals(value)) return true;
        return false;
    }

    private static String detectProfileZodiac(String profile, String[] zodiac) {
        String value = clean(profile);
        for (String sign : zodiac) {
            if (value.contains(sign)) return sign;
        }
        Matcher labeled = Pattern.compile(
                "(?:誕生日|生年月日|生まれ)[^0-9]{0,20}"
                        + "(?:(?:[12][0-9]{3})\\s*[年/.-]\\s*)?"
                        + "(\\d{1,2})\\s*[月/.-]\\s*(\\d{1,2})(?:\\s*日)?")
                .matcher(value);
        if (labeled.find()) {
            return zodiacForDate(parseInt(labeled.group(1)), parseInt(labeled.group(2)));
        }
        Matcher born = Pattern.compile(
                "(?:(?:[12][0-9]{3})\\s*[年/.-]\\s*)?"
                        + "(\\d{1,2})\\s*[月/.-]\\s*(\\d{1,2})\\s*日?\\s*(?:生まれ|生)")
                .matcher(value);
        if (born.find()) {
            return zodiacForDate(parseInt(born.group(1)), parseInt(born.group(2)));
        }
        return "";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String zodiacForDate(int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) return "";
        int code = month * 100 + day;
        if (code >= 321 && code <= 419) return "牡羊座";
        if (code >= 420 && code <= 520) return "牡牛座";
        if (code >= 521 && code <= 621) return "双子座";
        if (code >= 622 && code <= 722) return "蟹座";
        if (code >= 723 && code <= 822) return "獅子座";
        if (code >= 823 && code <= 922) return "乙女座";
        if (code >= 923 && code <= 1023) return "天秤座";
        if (code >= 1024 && code <= 1122) return "蠍座";
        if (code >= 1123 && code <= 1221) return "射手座";
        if (code >= 1222 || code <= 119) return "山羊座";
        if (code <= 218) return "水瓶座";
        return "魚座";
    }

    private static String buildScript(LocalDate date, String slotLabel, String region,
                                      SharedPreferences prefs, JSONArray events,
                                      JSONArray mails, JSONObject gmailUnread,
                                      HourlyForecast homeHourly,
                                      JSONArray eventWeather, JSONArray topNews,
                                      JSONArray localNews, JSONArray localEvents,
                                      JSONArray horoscope, JSONArray trivia) throws Exception {
        StringBuilder out = new StringBuilder();
        String dateLabel = DateTimeFormatter.ofPattern("M月d日、EEEE", Locale.JAPAN).format(date);
        out.append("電脳秘書ロキがお届けする、").append(slotLabel)
                .append("のロキ・トピック。")
                .append(dateLabel).append("の新しい情報をまとめてお伝えします。\n\n");
        if (horoscope.length() > 0) {
            JSONObject item = horoscope.optJSONObject(0);
            if (item != null) {
                if (item.optBoolean("generic", false)) {
                    out.append("最初に、全星座共通の今日の総合運です。占いは娯楽としてお楽しみください。\n")
                            .append(item.optString("fortune", "")).append("。\n\n");
                } else {
                    out.append("最初に、").append(item.optString("sign", "星座未設定"))
                            .append("の今日の星座占いです。占いは娯楽としてお楽しみください。\n")
                            .append(item.optString("sign", "")).append("は全体の")
                            .append(item.optInt("rank", 1)).append("位。")
                            .append(item.optString("fortune", "")).append("。\n\n");
                }
            }
        }
        out.append(horoscope.length() > 0
                ? "続いて、今日の予定です。\n"
                : "最初に、今日の予定です。\n");
        if (events.length() == 0) out.append("登録されている予定はありません。\n");
        long now = System.currentTimeMillis();
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            long eventBegin = event.optLong("begin", 0L);
            long eventEnd = event.optLong("end", 0L);
            String timing = event.optBoolean("allDay", false) ? "終日、"
                    : eventBegin <= now && eventEnd > now ? "現在進行中、"
                    : formatTime(eventBegin) + "から、";
            out.append(timing)
                    .append(event.optString("title", "予定"));
            out.append("。\n");
            for (int j = 0; j < eventWeather.length(); j++) {
                JSONObject weather = eventWeather.optJSONObject(j);
                if (weather != null && weather.optLong("begin", -1L) == event.optLong("begin", 0L)) {
                    String weatherRegion = clean(weather.optString("weatherRegion", region));
                    if (weatherRegion.length() == 0) weatherRegion = region;
                    out.append("この予定の時間帯は、").append(weatherRegion)
                            .append("の予報では、")
                            .append(weather.optString("text", "")).append("\n");
                    break;
                }
            }
        }

        out.append("\n続いて、").append(region).append("の天気です。\n");
        String condition = clean(prefs.getString(KEY_WEATHER_CONDITION, ""));
        String temperature = clean(prefs.getString(KEY_WEATHER_TEMPERATURE, ""));
        out.append(region).append("は").append(condition.length() == 0 ? "天気情報を確認中" : condition);
        if (temperature.length() > 0) out.append("、現在の気温は").append(temperature).append("度");
        out.append("です。\n");
        JSONObject homeRain = rainAdvice(homeHourly,
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L,
                region);
        out.append(region).append("の時間帯別予報では、")
                .append(homeRain.optString("text", "時間帯別予報は取得できませんでした。"))
                .append("\n");
        try {
            JSONArray daily = new JSONArray(prefs.getString(KEY_WEATHER_FORECAST, "[]"));
            JSONObject today = daily.optJSONObject(0);
            if (today != null) {
                out.append(region).append("の予想最高気温は")
                        .append(today.optString("max", "不明"))
                        .append("度、最低気温は").append(today.optString("min", "不明"))
                        .append("度、日中の最大降水確率は")
                        .append(today.optInt("rain", 0)).append("パーセントです。\n");
            }
        } catch (Exception ignored) {
        }

        out.append("\nメール通知から確認できる、まだ案内していないメール候補は")
                .append(mails.length()).append("件です。\n");
        for (int i = 0; i < mails.length(); i++) {
            JSONObject mail = mails.optJSONObject(i);
            if (mail == null) continue;
            out.append(i + 1).append("件目、")
                    .append(mail.optString("from_or_title", "送信者不明")).append("。")
                    .append(shortText(mail.optString("summary", ""), 150)).append("。\n");
        }
        if (mails.length() == 0) {
            if (gmailUnread.optBoolean("ok", false)
                    && gmailUnread.optInt("unreadInbox", 0) > 0) {
                out.append("Gmail受信トレイには未読が")
                        .append(gmailUnread.optInt("unreadInbox", 0))
                        .append("件あります。ただし通知が残っていないため、件名と本文は取得できません。\n");
            } else {
                out.append("新しく案内できるメール通知はありません。Gmail上の未読件数そのものではありません。\n");
            }
        }

        appendNewsSection(out, "ここからは主なニュースです。", topNews);
        appendNewsSection(out, region + "周辺の地域ニュースです。", localNews);
        appendEventSection(out, "今日から30日以内の周辺イベント情報です。", localEvents);
        appendTriviaSection(out, trivia);

        out.append("以上、").append(slotLabel)
                .append("のロキ・トピックでした。詳しく知りたい項目は、いつでもロキに聞いてください。");
        return out.toString();
    }

    private static void appendNewsSection(StringBuilder out, String heading,
                                          JSONArray articles) {
        out.append("\n").append(heading).append("\n");
        if (articles.length() == 0) {
            out.append("この項目は取得できませんでした。\n");
            return;
        }
        for (int i = 0; i < articles.length(); i++) {
            JSONObject article = articles.optJSONObject(i);
            if (article == null) continue;
            out.append(i + 1).append("、").append(article.optString("title", ""));
            out.append("。\n");
        }
    }

    private static void appendEventSection(StringBuilder out, String heading,
                                           JSONArray events) {
        out.append("\n").append(heading).append("\n");
        if (events.length() == 0) {
            out.append("開催日を確認できる新しいイベント情報はありません。\n");
            return;
        }
        for (int index = 0; index < events.length(); index++) {
            JSONObject event = events.optJSONObject(index);
            if (event == null) continue;
            out.append(index + 1).append("、")
                    .append(event.optString("eventDateLabel", "開催日未確認"))
                    .append("、").append(event.optString("title", ""));
            String source = clean(event.optString("source", ""));
            if (source.length() > 0) out.append("。情報元は").append(source);
            out.append("。\n");
        }
    }

    private static void appendTriviaSection(StringBuilder out, JSONArray trivia) {
        if (trivia == null || trivia.length() == 0) return;
        JSONObject item = trivia.optJSONObject(0);
        if (item == null) return;
        out.append("\n新着トピックが少なめなので、ロキの知識コラムをお届けします。\n")
                .append(item.optString("title", "今日の雑学")).append("。")
                .append(item.optString("text", "")).append("。\n");
    }

    private static String buildSummary(LocalDate date, String region, JSONArray events,
                                       HourlyForecast weather, JSONArray topNews,
                                       JSONArray localNews, JSONArray horoscope,
                                       JSONArray skippedTopNews,
                                       JSONArray skippedLocalNews,
                                       JSONArray skippedLocalEvents,
                                       JSONArray trivia) throws Exception {
        StringBuilder summary = new StringBuilder();
        summary.append(date).append(" ロキ・トピック要約。地域=").append(region)
                .append("。予定=");
        for (int i = 0; i < events.length() && i < 8; i++) {
            JSONObject event = events.optJSONObject(i);
            if (event != null) summary.append(event.optString("title", "")).append("、");
        }
        summary.append("主なニュース=");
        for (int i = 0; i < topNews.length() && i < 4; i++) {
            JSONObject article = topNews.optJSONObject(i);
            if (article != null) summary.append(shortText(article.optString("title", ""), 80)).append("／");
        }
        summary.append("地域ニュース=");
        for (int i = 0; i < localNews.length() && i < 3; i++) {
            JSONObject article = localNews.optJSONObject(i);
            if (article != null) summary.append(shortText(article.optString("title", ""), 70)).append("／");
        }
        JSONObject first = horoscope.optJSONObject(0);
        if (first != null) {
            summary.append("占い=");
            if (first.optBoolean("generic", false)) {
                summary.append("今日の総合運");
            } else {
                summary.append(first.optString("sign", ""))
                        .append(first.optInt("rank", 0)).append("位");
            }
        }
        JSONObject triviaItem = trivia == null ? null : trivia.optJSONObject(0);
        if (triviaItem != null) summary.append("。小話=")
                .append(shortText(triviaItem.optString("title", ""), 60));
        appendSkippedSummary(summary, "既報ニュース", skippedTopNews, 4);
        appendSkippedSummary(summary, "既報地域ニュース", skippedLocalNews, 3);
        appendSkippedSummary(summary, "既報イベント", skippedLocalEvents, 3);
        return shortText(summary.toString(), 1800);
    }

    private static void appendSkippedSummary(StringBuilder summary, String label,
                                             JSONArray items, int max) {
        if (items == null || items.length() == 0) return;
        summary.append("。").append(label).append("=");
        for (int index = 0; index < items.length() && index < max; index++) {
            JSONObject item = items.optJSONObject(index);
            if (item != null) {
                summary.append(shortText(item.optString("title", ""), 70)).append("／");
            }
        }
    }

    private static String formatTime(long millis) {
        return new java.text.SimpleDateFormat("H時mm分", Locale.JAPAN).format(new Date(millis))
                .replace("時00分", "時");
    }

    private static String normalizePlace(String place) {
        return clean(place).replaceAll("[\\s　]+", "").toLowerCase(Locale.JAPAN);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void writeAtomically(Context context, String json) throws Exception {
        File target = new File(context.getFilesDir(), FILE_NAME);
        File temporary = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        FileOutputStream output = new FileOutputStream(temporary, false);
        try {
            output.write(json.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } finally {
            output.close();
        }
        if (target.exists() && !target.delete()) {
            throw new IllegalStateException("old morning briefing could not be replaced");
        }
        if (!temporary.renameTo(target)) {
            throw new IllegalStateException("morning briefing rename failed");
        }
    }

    private static String fetchText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(urlText.contains("rss") ? 2500 : 8000);
        connection.setReadTimeout(urlText.contains("rss") ? 3000 : 10000);
        connection.setRequestProperty("User-Agent", "DennoHishoLoki/1.0");
        try {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String text = readAll(stream);
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return text;
        } finally { connection.disconnect(); }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line).append('\n');
        reader.close();
        return out.toString();
    }

    private static String extractXml(String text, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + "[^>]*>(.*?)</" + tag + ">",
                Pattern.DOTALL).matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).replaceAll("<!\\[CDATA\\[(.*?)\\]\\]>", "$1") : "";
    }

    private static String decodeXml(String value) {
        return clean(value).replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&apos;", "'");
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    private static String shortText(String value, int max) {
        String text = clean(value);
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private static final class HourlyForecast {
        final JSONArray times;
        final JSONArray rain;
        final JSONArray temperatures;

        HourlyForecast(JSONArray times, JSONArray rain, JSONArray temperatures) {
            this.times = times;
            this.rain = rain;
            this.temperatures = temperatures;
        }
    }

    private static final class GeocodedPlace {
        final double latitude;
        final double longitude;
        final String regionLabel;

        GeocodedPlace(double latitude, double longitude, String regionLabel) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.regionLabel = regionLabel == null ? "" : regionLabel;
        }
    }
}
