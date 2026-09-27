package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Phone-only credentials. No Google content is written to disk. */
final class GoogleNavigationClient {
    private static String session = "";
    private static long sessionExpires;
    static boolean enabled(Context c) {
        return c.getSharedPreferences("loki_maps_secure", 0).getBoolean("verified", false);
    }
    private static synchronized void reserve(Context c, String kind, int daily, int minute) throws Exception {
        SharedPreferences p = c.getSharedPreferences("loki_maps_usage", 0);
        long now = System.currentTimeMillis(), day = now / 86400000L, min = now / 60000L;
        int d = p.getLong(kind + "Day", -1) == day ? p.getInt(kind + "Daily", 0) : 0;
        int m = p.getLong(kind + "Minute", -1) == min ? p.getInt(kind + "Count", 0) : 0;
        if (d >= daily || m >= minute) throw new Exception("google_" + kind + "_local_limit");
        if (!p.edit().putLong(kind + "Day", day).putInt(kind + "Daily", d + 1)
                .putLong(kind + "Minute", min).putInt(kind + "Count", m + 1).commit())
            throw new Exception("google_usage_save_failed");
    }
    private static final class Response {
        byte[] bytes;
        String cache;
    }
    private static Response request(Context c, String endpoint, JSONObject body, String mask) throws Exception {
        if (!enabled(c)) throw new Exception("google_maps_not_verified");
        HttpURLConnection connection = null;
        try {
            String key = MapsKeyInput.normalize(MapsCredentialStore.read(c));
            String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "key=" + URLEncoder.encode(key, "UTF-8");
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(9000);
            connection.setUseCaches(false);
            connection.setRequestProperty("X-Android-Package", c.getPackageName());
            connection.setRequestProperty("X-Android-Cert", GoogleMapsConnectionCheck.fingerprint(c));
            connection.setRequestProperty("X-Goog-Api-Key", key);
            if (mask != null) connection.setRequestProperty("X-Goog-FieldMask", mask);
            if (body != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(data.length);
                try (java.io.OutputStream out = connection.getOutputStream()) { out.write(data); }
            }
            int code = connection.getResponseCode();
            if (code != 200) throw new Exception("google_http_" + code);
            Response result = new Response();
            result.cache = connection.getHeaderField("Cache-Control");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (out.size() + read > 2097152) throw new Exception("google_response_too_large");
                    out.write(buffer, 0, read);
                }
                result.bytes = out.toByteArray();
            }
            return result;
        } catch (Exception error) {
            // Exceptions from HTTP clients may contain the key-bearing URL.
            String reason = error.getMessage();
            throw new Exception(reason != null && reason.matches("google_[a-z_0-9]+")
                    ? reason : "google_network_error");
        } finally { if (connection != null) connection.disconnect(); }
    }
    static java.util.List<android.location.Address> searchPlaces(Context c, String query) throws Exception {
        if (query == null || query.trim().isEmpty() || query.length() > 200)
            throw new Exception("google_invalid_query");
        reserve(c, "places", 10, 3);
        JSONObject body = new JSONObject().put("textQuery", query.trim()).put("languageCode", "ja")
                .put("regionCode", "JP").put("pageSize", 10);
        Response response = request(c, "https://places.googleapis.com/v1/places:searchText", body,
                "places.id,places.displayName,places.formattedAddress,places.location");
        JSONArray places = new JSONObject(new String(response.bytes, StandardCharsets.UTF_8)).optJSONArray("places");
        java.util.List<android.location.Address> found = new java.util.ArrayList<>();
        if (places == null) return found;
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i), location = place.optJSONObject("location");
            if (location == null) continue;
            double lat = location.optDouble("latitude", Double.NaN), lng = location.optDouble("longitude", Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) continue;
            String id = place.optString("id", lat + "," + lng);
            if (!seen.add(id)) continue;
            JSONObject display = place.optJSONObject("displayName");
            String name = display == null ? "" : display.optString("text", "");
            String address = place.optString("formattedAddress", "");
            android.location.Address item = new android.location.Address(java.util.Locale.JAPAN);
            item.setLatitude(lat); item.setLongitude(lng); item.setFeatureName(name);
            item.setAddressLine(0, name.isEmpty() ? address : name + "\n" + address);
            found.add(item);
        }
        return found;
    }

    static JSONObject route(Context c, double lat, double lng, String destination, String mode) throws Exception {
        reserve(c, "routes", 20, 3);
        String travel = "walking".equals(mode) ? "WALK" : "bicycling".equals(mode) ? "BICYCLE"
                : "transit".equals(mode) ? "TRANSIT" : "DRIVE";
        JSONObject point = new JSONObject().put("latitude", lat).put("longitude", lng);
        JSONObject body = new JSONObject().put("origin", new JSONObject().put("location", new JSONObject().put("latLng", point)))
                .put("destination", new JSONObject().put("address", destination)).put("travelMode", travel)
                .put("languageCode", "ja-JP").put("units", "METRIC");
        String fields = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.startLocation,routes.legs.steps.distanceMeters,routes.legs.steps.staticDuration,routes.legs.steps.navigationInstruction";
        if (!"DRIVE".equals(travel)) fields += ",routes.legs.steps.travelMode,routes.legs.steps.polyline.encodedPolyline,routes.legs.steps.endLocation";
        if ("TRANSIT".equals(travel)) fields += ",routes.legs.steps.transitDetails";
        Response response = request(c, "https://routes.googleapis.com/directions/v2:computeRoutes", body, fields);
        JSONObject root = new JSONObject(new String(response.bytes, StandardCharsets.UTF_8));
        JSONArray routes = root.optJSONArray("routes");
        if (routes == null || routes.length() == 0) throw new Exception("google_route_not_found");
        return routes.getJSONObject(0);
    }
    private static synchronized String session(Context c) throws Exception {
        if (session.length() > 0 && System.currentTimeMillis() < sessionExpires - 60000) return session;
        reserve(c, "session", 30, 2);
        JSONObject body = new JSONObject().put("mapType", "roadmap").put("language", "ja-JP").put("region", "JP");
        body.put("styles", new JSONArray("[{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#000000\"}]},{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#54996a\"}]},{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"#000000\"}]},{\"featureType\":\"road\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#245b36\"}]},{\"featureType\":\"poi\",\"stylers\":[{\"visibility\":\"off\"}]}]"));
        JSONObject result = new JSONObject(new String(request(c, "https://tile.googleapis.com/v1/createSession", body, null).bytes, StandardCharsets.UTF_8));
        session = result.getString("session");
        sessionExpires = result.getLong("expiry") * 1000L;
        return session;
    }
    static JSONObject tile(Context c, int z, int x, int y) throws Exception {
        if (z < 1 || z > 20 || x < 0 || y < 0 || x >= (1 << z) || y >= (1 << z))
            throw new Exception("google_invalid_tile");
        String s = URLEncoder.encode(session(c), "UTF-8");
        reserve(c, "tiles", 1800, 90);
        Response tile = request(c, "https://tile.googleapis.com/v1/2dtiles/" + z + "/" + x + "/" + y + "?session=" + s, null, null);
        double n = 1 << z;
        double north = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n))));
        double south = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1) / n))));
        JSONObject viewport = new JSONObject(new String(request(c, "https://tile.googleapis.com/tile/v1/viewport?session=" + s
                + "&zoom=" + z + "&north=" + north + "&south=" + south + "&west=" + (x / n * 360 - 180)
                + "&east=" + ((x + 1) / n * 360 - 180), null, null).bytes, StandardCharsets.UTF_8));
        String copyright = viewport.getString("copyright");
        return new JSONObject().put("ok", true).put("image", Base64.encodeToString(tile.bytes, Base64.NO_WRAP))
                .put("copyright", copyright).put("cacheControl", tile.cache == null ? "no-store" : tile.cache);
    }
}
