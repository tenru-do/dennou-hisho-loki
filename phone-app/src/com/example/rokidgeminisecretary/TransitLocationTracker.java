package com.example.rokidgeminisecretary;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Estimates the current railway line and next station from phone GPS samples.
 *
 * Google Maps navigation notifications remain the preferred source. This is a
 * fallback for trips where Maps does not publish usable transit guidance.
 * Station/line geometry is supplied by HeartRails Express.
 */
public final class TransitLocationTracker implements LocationListener {
    private static final String TAG = "RokidTransit";
    private static final String PREFS = "phone_secretary";
    private static final String KEY_COMPACT = "transit_gps_compact";
    private static final String KEY_TIME = "transit_gps_time";
    private static final String KEY_LINE = "transit_gps_line";
    private static final String KEY_STATION = "transit_gps_station";
    private static final String KEY_SPEED = "transit_gps_speed";
    private static final String KEY_HEADING = "transit_gps_heading";
    private static final String KEY_CONFIDENCE = "transit_gps_confidence";
    private static final String KEY_DEBUG = "transit_gps_debug";

    private static final long QUERY_INTERVAL_MS = 15000L;
    private static final long RESULT_VALID_MS = 240000L;
    private static final long LINE_LOCK_MS = 30L * 60L * 1000L;
    private static final long LINE_CACHE_MS = 24L * 60L * 60L * 1000L;
    private static final float MIN_TRANSIT_SPEED_MPS = 4.2f;
    private static final float MIN_HEADING_DISTANCE_M = 25.0f;
    private static final long IDLE_GPS_INTERVAL_MS = 120000L;
    private static final long IDLE_NETWORK_INTERVAL_MS = 60000L;
    private static final long ACTIVE_GPS_INTERVAL_MS = 5000L;
    private static final long ACTIVE_NETWORK_INTERVAL_MS = 10000L;
    private static final long RETURN_TO_IDLE_MS = 120000L;

    private static TransitLocationTracker instance;

    private final Context context;
    private final LocationManager locationManager;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<String, CachedLine> lineCache = new HashMap<String, CachedLine>();
    private boolean registered;
    private boolean highRateTracking;
    private volatile boolean queryInFlight;
    private Location previousLocation;
    private Location lastQueryLocation;
    private long lastQueryAt;
    private long lastMovingAt;
    private float smoothedSpeed;
    private float smoothedHeading = Float.NaN;
    private String pendingPredictionKey = "";
    private int pendingPredictionHits;

    private TransitLocationTracker(Context value) {
        context = value.getApplicationContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static synchronized void start(Context context) {
        if (instance == null) {
            instance = new TransitLocationTracker(context);
        }
        instance.startInternal();
    }

    public static synchronized void stop() {
        if (instance != null) {
            instance.stopInternal();
            instance = null;
        }
    }

    public static JSONObject recentTransitJson(Context context) throws Exception {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long time = preferences.getLong(KEY_TIME, 0L);
        String compact = preferences.getString(KEY_COMPACT, "").trim();
        boolean fresh = compact.length() > 0 && time > 0L
                && System.currentTimeMillis() - time <= RESULT_VALID_MS;
        JSONObject root = new JSONObject();
        root.put("ok", fresh);
        root.put("compact", fresh ? compact : "");
        root.put("time", fresh ? time : 0L);
        root.put("source", fresh ? "HeartRails Express + phone GPS" : "");
        root.put("estimated", fresh);
        if (fresh) {
            root.put("line", preferences.getString(KEY_LINE, ""));
            root.put("nextStation", preferences.getString(KEY_STATION, ""));
            root.put("speedMps", preferences.getFloat(KEY_SPEED, 0.0f));
            root.put("heading", preferences.getFloat(KEY_HEADING, 0.0f));
            root.put("confidence", preferences.getString(KEY_CONFIDENCE, ""));
            root.put("debug", preferences.getString(KEY_DEBUG, ""));
        }
        return root;
    }

    private synchronized void startInternal() {
        if (registered || locationManager == null || !hasLocationPermission()) {
            return;
        }
        try {
            registerLocationUpdates(false);
            registered = true;
            Log.i(TAG, "GPS transit estimation started in idle power mode");
        } catch (SecurityException error) {
            Log.w(TAG, "location permission unavailable", error);
        } catch (Exception error) {
            Log.w(TAG, "location tracking could not start", error);
        }
    }

    private synchronized void registerLocationUpdates(boolean highRate) {
        try {
            locationManager.removeUpdates(this);
        } catch (Exception ignored) {
        }
        long gpsInterval = highRate ? ACTIVE_GPS_INTERVAL_MS : IDLE_GPS_INTERVAL_MS;
        long networkInterval = highRate ? ACTIVE_NETWORK_INTERVAL_MS : IDLE_NETWORK_INTERVAL_MS;
        float gpsDistance = highRate ? 10.0f : 50.0f;
        float networkDistance = highRate ? 25.0f : 75.0f;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    gpsInterval, gpsDistance, this, Looper.getMainLooper());
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    networkInterval, networkDistance, this, Looper.getMainLooper());
        }
        highRateTracking = highRate;
        Log.i(TAG, highRate ? "transit tracking switched to moving mode"
                : "transit tracking switched to idle power mode");
    }

    private synchronized void stopInternal() {
        if (registered && locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
        registered = false;
        worker.shutdownNow();
    }

    private boolean hasLocationPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null || location.getLatitude() < 20.0 || location.getLatitude() > 46.5
                || location.getLongitude() < 122.0 || location.getLongitude() > 154.0) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Location prior;
        final float speed;
        final float heading;
        synchronized (this) {
            prior = previousLocation;
            float derivedSpeed = 0.0f;
            float derivedHeading = Float.NaN;
            if (prior != null) {
                long elapsed = location.getTime() - prior.getTime();
                float distance = prior.distanceTo(location);
                float uncertainty = Math.max(prior.hasAccuracy() ? prior.getAccuracy() : 0.0f,
                        location.hasAccuracy() ? location.getAccuracy() : 0.0f);
                float reliableDistance = Math.max(MIN_HEADING_DISTANCE_M, uncertainty * 1.5f);
                if (elapsed >= 1500L && elapsed <= 60000L && uncertainty <= 120.0f
                        && distance >= reliableDistance) {
                    derivedSpeed = distance / (elapsed / 1000.0f);
                    derivedHeading = prior.bearingTo(location);
                }
            }
            float rawSpeed = location.hasSpeed() ? location.getSpeed() : derivedSpeed;
            if (derivedSpeed > rawSpeed && derivedSpeed < 100.0f) {
                rawSpeed = derivedSpeed;
            }
            if (rawSpeed < 0.0f || rawSpeed > 100.0f) {
                rawSpeed = 0.0f;
            }
            smoothedSpeed = smoothedSpeed <= 0.0f
                    ? rawSpeed : smoothedSpeed * 0.58f + rawSpeed * 0.42f;
            float rawHeading = location.hasBearing() && location.getSpeed() >= 1.5f
                    ? location.getBearing() : derivedHeading;
            if (!Float.isNaN(rawHeading)) {
                smoothedHeading = Float.isNaN(smoothedHeading)
                        ? normalizeBearing(rawHeading)
                        : blendBearing(smoothedHeading, rawHeading, 0.45f);
            }
            previousLocation = new Location(location);
            speed = smoothedSpeed;
            heading = smoothedHeading;
            if (speed >= MIN_TRANSIT_SPEED_MPS) {
                lastMovingAt = now;
            }
        }

        if (speed >= MIN_TRANSIT_SPEED_MPS && !highRateTracking) {
            registerLocationUpdates(true);
        } else if (highRateTracking && lastMovingAt > 0L
                && now - lastMovingAt > RETURN_TO_IDLE_MS) {
            registerLocationUpdates(false);
        }

        if (speed < MIN_TRANSIT_SPEED_MPS || Float.isNaN(heading)) {
            if (lastMovingAt > 0L && now - lastMovingAt > RESULT_VALID_MS) {
                clearPrediction("movement stopped");
            }
            return;
        }
        float movedSinceQuery = lastQueryLocation == null
                ? Float.MAX_VALUE : lastQueryLocation.distanceTo(location);
        if (queryInFlight || (now - lastQueryAt < QUERY_INTERVAL_MS && movedSinceQuery < 250.0f)) {
            return;
        }
        lastQueryAt = now;
        lastQueryLocation = new Location(location);
        queryInFlight = true;
        final Location snapshot = new Location(location);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    Prediction prediction = estimateNextStation(snapshot, speed, heading);
                    if (prediction != null) {
                        publishPrediction(prediction, speed, heading);
                    } else {
                        Log.d(TAG, String.format(Locale.JAPAN,
                                "no rail match speed=%.1f heading=%.0f accuracy=%.0f",
                                speed, heading, snapshot.getAccuracy()));
                    }
                } catch (Exception error) {
                    Log.w(TAG, "next-station estimation failed", error);
                } finally {
                    queryInFlight = false;
                }
            }
        });
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    private Prediction estimateNextStation(Location location, float speed, float heading)
            throws Exception {
        JSONArray nearby = fetchStationArray("https://express.heartrails.com/api/json?method=getStations&x="
                + String.format(Locale.US, "%.6f", location.getLongitude())
                + "&y=" + String.format(Locale.US, "%.6f", location.getLatitude()));
        LinkedHashSet<String> lines = new LinkedHashSet<String>();
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long savedAt = preferences.getLong(KEY_TIME, 0L);
        String savedLine = preferences.getString(KEY_LINE, "").trim();
        if (savedLine.length() > 0 && System.currentTimeMillis() - savedAt <= LINE_LOCK_MS) {
            lines.add(savedLine);
        }
        for (int index = 0; index < nearby.length() && lines.size() < 6; index++) {
            JSONObject station = nearby.optJSONObject(index);
            if (station == null) continue;
            String line = station.optString("line", "").trim();
            if (line.length() > 0) lines.add(line);
        }

        Prediction best = null;
        float allowedTrackDistance = Math.max(260.0f,
                Math.min(750.0f, location.hasAccuracy() ? location.getAccuracy() * 2.5f : 350.0f));
        for (String line : lines) {
            List<Station> stations = getLineStations(line);
            for (int index = 0; index < stations.size(); index++) {
                Station first = stations.get(index);
                Station second = findStation(stations, first.nextName);
                if (second == null && index + 1 < stations.size()) {
                    second = stations.get(index + 1);
                }
                if (second == null || second.name.equals(first.name)) continue;
                double trackDistance = distanceToSegmentMeters(location.getLatitude(),
                        location.getLongitude(), first.latitude, first.longitude,
                        second.latitude, second.longitude);
                if (trackDistance > allowedTrackDistance) continue;
                float forward = bearingDegrees(first.latitude, first.longitude,
                        second.latitude, second.longitude);
                float forwardDifference = angleDifference(heading, forward);
                float reverseDifference = angleDifference(heading, normalizeBearing(forward + 180.0f));
                float headingDifference = Math.min(forwardDifference, reverseDifference);
                if (headingDifference > 52.0f) continue;
                Station target = forwardDifference <= reverseDifference ? second : first;
                float targetDistance = distanceMeters(location.getLatitude(), location.getLongitude(),
                        target.latitude, target.longitude);
                if (targetDistance > 30000.0f) continue;
                double score = trackDistance * 1.7 + headingDifference * 24.0
                        + Math.min(targetDistance, 10000.0f) * 0.025;
                if (line.equals(savedLine)) score -= 140.0;
                String confidence = trackDistance <= 140.0 && headingDifference <= 22.0f
                        ? "high" : trackDistance <= 350.0 && headingDifference <= 38.0f
                        ? "medium" : "low";
                Prediction candidate = new Prediction(line, target.name, confidence,
                        trackDistance, headingDifference, targetDistance, score);
                if (best == null || candidate.score < best.score) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private List<Station> getLineStations(String line) throws Exception {
        CachedLine cached = lineCache.get(line);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.time <= LINE_CACHE_MS) {
            return cached.stations;
        }
        String encoded = URLEncoder.encode(line, StandardCharsets.UTF_8.name());
        JSONArray array = fetchStationArray(
                "https://express.heartrails.com/api/json?method=getStations&line=" + encoded);
        ArrayList<Station> stations = new ArrayList<Station>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) continue;
            String name = item.optString("name", "").trim();
            String previous = item.isNull("prev") ? "" : item.optString("prev", "").trim();
            String next = item.isNull("next") ? "" : item.optString("next", "").trim();
            double longitude = item.optDouble("x", Double.NaN);
            double latitude = item.optDouble("y", Double.NaN);
            if (name.length() > 0 && !Double.isNaN(longitude) && !Double.isNaN(latitude)) {
                stations.add(new Station(name, previous, next, latitude, longitude));
            }
        }
        lineCache.put(line, new CachedLine(now, stations));
        return stations;
    }

    private Station findStation(List<Station> stations, String name) {
        if (name == null || name.length() == 0) return null;
        for (Station station : stations) {
            if (name.equals(station.name)) return station;
        }
        return null;
    }

    private JSONArray fetchStationArray(String url) throws Exception {
        JSONObject root = new JSONObject(fetchText(url));
        JSONObject response = root.optJSONObject("response");
        JSONArray stations = response == null ? null : response.optJSONArray("station");
        if (stations == null) {
            throw new IllegalStateException("HeartRails response has no station data");
        }
        return stations;
    }

    private String fetchText(String value) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "DennouHishoLoki/0.9 transit-estimator");
        try {
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IllegalStateException("HTTP " + code);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + " " + body);
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private synchronized void publishPrediction(Prediction prediction, float speed, float heading) {
        String key = prediction.line + "|" + prediction.nextStation;
        if (key.equals(pendingPredictionKey)) {
            pendingPredictionHits++;
        } else {
            pendingPredictionKey = key;
            pendingPredictionHits = 1;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedLine = preferences.getString(KEY_LINE, "");
        long savedAt = preferences.getLong(KEY_TIME, 0L);
        boolean lockedLine = prediction.line.equals(savedLine)
                && System.currentTimeMillis() - savedAt <= LINE_LOCK_MS;
        if (!"high".equals(prediction.confidence) && !lockedLine && pendingPredictionHits < 2) {
            Log.d(TAG, "waiting for a second matching GPS estimate: " + key);
            return;
        }
        String compact = "推定 " + prediction.line + " 次 " + prediction.nextStation;
        String debug = String.format(Locale.JAPAN,
                "track=%.0fm directionDiff=%.0fdeg target=%.0fm",
                prediction.trackDistance, prediction.headingDifference,
                prediction.targetDistance);
        preferences.edit()
                .putString(KEY_COMPACT, compact)
                .putLong(KEY_TIME, System.currentTimeMillis())
                .putString(KEY_LINE, prediction.line)
                .putString(KEY_STATION, prediction.nextStation)
                .putFloat(KEY_SPEED, speed)
                .putFloat(KEY_HEADING, heading)
                .putString(KEY_CONFIDENCE, prediction.confidence)
                .putString(KEY_DEBUG, debug)
                .apply();
        Log.i(TAG, compact + " confidence=" + prediction.confidence + " " + debug);
    }

    private synchronized void clearPrediction(String reason) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getString(KEY_COMPACT, "").length() == 0) return;
        preferences.edit().remove(KEY_COMPACT).remove(KEY_TIME).remove(KEY_LINE)
                .remove(KEY_STATION).remove(KEY_SPEED).remove(KEY_HEADING)
                .remove(KEY_CONFIDENCE).remove(KEY_DEBUG).apply();
        pendingPredictionKey = "";
        pendingPredictionHits = 0;
        Log.i(TAG, "GPS transit estimate cleared: " + reason);
    }

    private static float blendBearing(float current, float update, float weight) {
        double currentRadians = Math.toRadians(current);
        double updateRadians = Math.toRadians(update);
        double x = Math.cos(currentRadians) * (1.0 - weight) + Math.cos(updateRadians) * weight;
        double y = Math.sin(currentRadians) * (1.0 - weight) + Math.sin(updateRadians) * weight;
        return normalizeBearing((float) Math.toDegrees(Math.atan2(y, x)));
    }

    private static float normalizeBearing(float value) {
        float normalized = value % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private static float angleDifference(float first, float second) {
        float difference = Math.abs(normalizeBearing(first) - normalizeBearing(second));
        return difference > 180.0f ? 360.0f - difference : difference;
    }

    private static float bearingDegrees(double fromLatitude, double fromLongitude,
                                        double toLatitude, double toLongitude) {
        double first = Math.toRadians(fromLatitude);
        double second = Math.toRadians(toLatitude);
        double longitudeDifference = Math.toRadians(toLongitude - fromLongitude);
        double y = Math.sin(longitudeDifference) * Math.cos(second);
        double x = Math.cos(first) * Math.sin(second)
                - Math.sin(first) * Math.cos(second) * Math.cos(longitudeDifference);
        return normalizeBearing((float) Math.toDegrees(Math.atan2(y, x)));
    }

    private static float distanceMeters(double firstLatitude, double firstLongitude,
                                        double secondLatitude, double secondLongitude) {
        float[] result = new float[1];
        Location.distanceBetween(firstLatitude, firstLongitude,
                secondLatitude, secondLongitude, result);
        return result[0];
    }

    private static double distanceToSegmentMeters(double latitude, double longitude,
                                                  double firstLatitude, double firstLongitude,
                                                  double secondLatitude, double secondLongitude) {
        double referenceLatitude = Math.toRadians(latitude);
        double x = (longitude - firstLongitude) * 111320.0 * Math.cos(referenceLatitude);
        double y = (latitude - firstLatitude) * 110540.0;
        double segmentX = (secondLongitude - firstLongitude) * 111320.0
                * Math.cos(referenceLatitude);
        double segmentY = (secondLatitude - firstLatitude) * 110540.0;
        double squaredLength = segmentX * segmentX + segmentY * segmentY;
        if (squaredLength <= 1.0) return Math.sqrt(x * x + y * y);
        double projection = (x * segmentX + y * segmentY) / squaredLength;
        projection = Math.max(0.0, Math.min(1.0, projection));
        double deltaX = x - segmentX * projection;
        double deltaY = y - segmentY * projection;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static final class Station {
        final String name;
        final String previousName;
        final String nextName;
        final double latitude;
        final double longitude;

        Station(String name, String previousName, String nextName,
                double latitude, double longitude) {
            this.name = name;
            this.previousName = previousName;
            this.nextName = nextName;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final class CachedLine {
        final long time;
        final List<Station> stations;

        CachedLine(long time, List<Station> stations) {
            this.time = time;
            this.stations = stations;
        }
    }

    private static final class Prediction {
        final String line;
        final String nextStation;
        final String confidence;
        final double trackDistance;
        final float headingDifference;
        final float targetDistance;
        final double score;

        Prediction(String line, String nextStation, String confidence,
                   double trackDistance, float headingDifference,
                   float targetDistance, double score) {
            this.line = line;
            this.nextStation = nextStation;
            this.confidence = confidence;
            this.trackDistance = trackDistance;
            this.headingDifference = headingDifference;
            this.targetDistance = targetDistance;
            this.score = score;
        }
    }
}
