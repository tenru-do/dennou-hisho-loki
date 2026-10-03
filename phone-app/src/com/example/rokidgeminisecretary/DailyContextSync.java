package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.*;
import android.location.*;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.*;

/** Each source owns its executor, deadline and cache; glass polls caches independently. */
final class DailyContextSync implements SensorEventListener, AutoCloseable {
    static final ContextSyncPolicy.Reporter LOG =
            (part, error) -> Log.e("[ContextSync]", part + " FAIL " + error.getClass().getName(), error);
    private final Context context;
    private final SharedPreferences prefs;
    private final SensorManager sensors;
    private final ContextSyncJobs jobs = new ContextSyncJobs();
    private volatile boolean closed;
    DailyContextSync(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("phone_secretary", Context.MODE_PRIVATE);
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        schedule("STEPS", this::registerSteps, 60);
        schedule("STEPS/HealthConnect", this::readHealthConnect, 60);
        schedule("LOCATION", this::readLocation, 60);
        schedule("WEATHER", this::readWeather, 60);
    }
    private void schedule(String part,
                          ContextSyncPolicy.Task<Boolean> task, int seconds) {
        jobs.add(part, () -> {
            if (closed) return false;
            Boolean result = task.run();
            if (Boolean.TRUE.equals(result)) Log.i("[ContextSync]", part + " SUCCESS");
            return result;
        }, seconds, TimeUnit.SECONDS, LOG);
    }
    private boolean registered;
    private volatile long healthQueryAt;
    private Boolean readHealthConnect() {
        if (android.os.Build.VERSION.SDK_INT < 34) return false;
        if (context.checkSelfPermission("android.permission.health.READ_STEPS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Health Connect READ_STEPS missing; Step Counter continues");
        if (System.currentTimeMillis() - healthQueryAt < 60000L) return false;
        android.health.connect.HealthConnectManager manager =
                context.getSystemService(android.health.connect.HealthConnectManager.class);
        if (manager == null) throw new IllegalStateException("Health Connect unavailable");
        String day = LocalDate.now().toString();
        android.health.connect.TimeInstantRangeFilter range = new android.health.connect.TimeInstantRangeFilter.Builder()
                .setStartTime(LocalDate.parse(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                .setEndTime(java.time.Instant.now()).build();
        android.health.connect.AggregateRecordsRequest<Long> request =
                new android.health.connect.AggregateRecordsRequest.Builder<Long>(range)
                .addAggregationType(android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL).build();
        healthQueryAt = System.currentTimeMillis();
        manager.aggregate(request, context.getMainExecutor(),
                new android.os.OutcomeReceiver<android.health.connect.AggregateRecordsResponse<Long>,
                        android.health.connect.HealthConnectException>() {
                    public void onResult(android.health.connect.AggregateRecordsResponse<Long> response) {
                        ContextSyncPolicy.attempt("STEPS/HealthConnect/callback", () -> {
                            if (closed || !day.equals(LocalDate.now().toString())) return false;
                            Long count = response.get(android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL);
                            if (count == null) throw new IllegalStateException("No Health Connect steps");
                            long now = System.currentTimeMillis();
                            prefs.edit().putString("health_date", day).putString("health_compact", "歩数 " + count)
                                    .putLong("health_time", now).putLong("health_connect_time", now).apply();
                            return true;
                        }, false, LOG);
                    }
                    public void onError(android.health.connect.HealthConnectException error) {
                        LOG.failed("STEPS/HealthConnect", error);
                    }
                });
        return true;
    }
    private Boolean registerSteps() {
        if (registered) return true;
        if (android.os.Build.VERSION.SDK_INT >= 29 && context.checkSelfPermission(
                "android.permission.ACTIVITY_RECOGNITION") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("ACTIVITY_RECOGNITION missing");
        if (sensors == null) throw new IllegalStateException("SensorManager unavailable");
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) throw new IllegalStateException("Step Counter unavailable");
        registered = sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL,
                new Handler(Looper.getMainLooper()));
        if (!registered) throw new IllegalStateException("Step Counter registration failed");
        return true;
    }
    private Boolean readLocation() {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) throw new IllegalStateException("LocationManager unavailable");
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            Location value = ContextSyncPolicy.attempt("LOCATION/" + provider,
                    () -> manager.getLastKnownLocation(provider), null, LOG);
            if (value != null && (best == null || value.getTime() > best.getTime())) best = value;
        }
        if (best == null || System.currentTimeMillis() - best.getTime() > 900000L)
            throw new IllegalStateException("No fresh location; tracker permission/provider required");
        prefs.edit().putString("context_lat", Double.toString(best.getLatitude()))
                .putString("context_lon", Double.toString(best.getLongitude()))
                .putLong("context_location_time", best.getTime()).apply();
        final Location snapshot = best;
        String place = ContextSyncPolicy.attempt("LOCATION/geocode", () -> {
            java.util.List<Address> addresses = new Geocoder(context, java.util.Locale.JAPAN)
                    .getFromLocation(snapshot.getLatitude(), snapshot.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) throw new IOException("No address");
            Address a = addresses.get(0);
            return (a.getAdminArea() == null ? "" : a.getAdminArea()) + " "
                    + (a.getLocality() == null ? "" : a.getLocality());
        }, "", LOG);
        if (!place.trim().isEmpty()) prefs.edit().putString("weather_location", place.trim()).apply();
        return true;
    }
    private Boolean readWeather() throws Exception {
        if (System.currentTimeMillis() - prefs.getLong("weather_time", 0L) < 600000L) return true;
        // Cached coordinates survive a location-job failure; never wait on that job.
        long at = prefs.getLong("context_location_time", 0);
        if (at == 0 || System.currentTimeMillis() - at > 86400000L)
            throw new IllegalStateException("No usable cached coordinates");
        double lat = Double.parseDouble(prefs.getString("context_lat", ""));
        double lon = Double.parseDouble(prefs.getString("context_lon", ""));
        HttpURLConnection c = (HttpURLConnection) new URL(
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code").openConnection();
        try {
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("Weather HTTP " + code);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buffer = new byte[2048]; int n;
                while ((n = in.read(buffer)) != -1) {
                    bytes.write(buffer, 0, n);
                    if (bytes.size() > 65536) throw new IOException("Weather response too large");
                }
            }
            JSONObject current = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8))
                    .getJSONObject("current");
            double temp = current.getDouble("temperature_2m");
            if (Double.isNaN(temp) || Double.isInfinite(temp)) throw new IOException("Invalid temperature");
            int wmo = current.getInt("weather_code");
            String condition = wmo == 0 ? "快晴" : wmo <= 2 ? "晴れ" : wmo == 3 ? "曇り"
                    : wmo <= 48 ? "霧" : wmo <= 67 ? "雨" : wmo <= 77 ? "雪"
                    : wmo <= 82 ? "にわか雨" : wmo <= 86 ? "にわか雪" : "雷雨";
            prefs.edit().putString("weather_condition", condition)
                    .putString("weather_temperature", String.format(java.util.Locale.JAPAN, "%.1f", temp))
                    .putLong("weather_time", System.currentTimeMillis()).apply();
            return true;
        } finally { c.disconnect(); }
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (closed) return;
        ContextSyncPolicy.attempt("STEPS/callback", () -> {
            if (event.values == null || event.values.length == 0 || Float.isNaN(event.values[0]))
                throw new IllegalStateException("Invalid step event");
            long total = (long) event.values[0];
            String day = LocalDate.now().toString();
            long base = prefs.getLong("sensor_step_base", total);
            if (!day.equals(prefs.getString("sensor_step_day", "")) || total < base) base = total;
            SharedPreferences.Editor editor = prefs.edit().putString("sensor_step_day", day)
                    .putLong("sensor_step_base", base);
            if (!day.equals(prefs.getString("health_date", ""))
                    || System.currentTimeMillis() - prefs.getLong("health_connect_time", 0L) > 180000L)
                editor.putString("health_date", day).putString("health_compact", "歩数 " + Math.max(0, total - base))
                        .putLong("health_time", System.currentTimeMillis());
            editor.apply();
            return true;
        }, false, LOG);
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void close() {
        closed = true;
        jobs.close();
        if (sensors != null) sensors.unregisterListener(this);
    }
}
