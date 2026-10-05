package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** No user coordinates/history are sent. Errors intentionally never contain URLs or keys. */
final class GoogleMapsConnectionCheck {
    private static final String ROUTE_BODY = "{\"origin\":{\"location\":{\"latLng\":{\"latitude\":35.681236,\"longitude\":139.767125}}},"
            + "\"destination\":{\"location\":{\"latLng\":{\"latitude\":35.6805,\"longitude\":139.766}}},\"travelMode\":\"WALK\"}";
    static synchronized String verify(Context context) throws Exception {
        android.content.SharedPreferences state = context.getSharedPreferences("loki_maps_secure", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - state.getLong("last_check", 0L) < 60000L)
            return "連続確認を抑制しています。1分後に再試行してください。";
        state.edit().putLong("last_check", now).putBoolean("verified", false).apply();
        String key = MapsKeyInput.normalize(MapsCredentialStore.read(context));
        if (key.length() == 0) return "Maps専用キーが未設定です。";
        if (!MapsKeyInput.canSend(key)) {
            String formatError = "保存された入力に途中の空白・改行・全角文字等があるか、512文字を超えています。認証リクエストは送っていません。";
            android.util.Log.i("LokiMapsCheck", formatError);
            return formatError;
        }
        String routeUrl = "https://routes.googleapis.com/directions/v2:computeRoutes";
        String tileUrl = "https://tile.googleapis.com/v1/createSession?key=" + java.net.URLEncoder.encode(key, "UTF-8");
        String sessionBody = "{\"mapType\":\"roadmap\",\"language\":\"ja-JP\",\"region\":\"JP\"}";
        // Fail closed if the service accepts a different Android application.
        Result route = post(context, routeUrl, key, ROUTE_BODY, true, false);
        Result tile = post(context, tileUrl, key, sessionBody, false, false);
        if (route.code != 200 || tile.code != 200) {
            String failure = "正規アプリの接続未完了（Routes " + route + " / Tiles " + tile + "）。制限は変更していません。";
            android.util.Log.i("LokiMapsCheck", failure);
            return failure;
        }
        Result routeDenied = post(context, routeUrl, key, ROUTE_BODY, true, true);
        Result tileDenied = post(context, tileUrl, key, sessionBody, false, true);
        boolean verified = routeDenied.appBlocked() && tileDenied.appBlocked();
        context.getSharedPreferences("loki_maps_secure", Context.MODE_PRIVATE).edit()
                .putBoolean("verified", verified).apply();
        String result = verified ? "両APIの接続・アプリ制限を確認しました。ナビ時にGoogle地図とAPI計算経路を使用します。"
                : "アプリ制限の拒否確認未完了（Routes " + routeDenied + " / Tiles " + tileDenied + "）。連携は停止しています。";
        android.util.Log.i("LokiMapsCheck", result);
        return result;
    }
    private static final class Result {
        final int code;
        final String reason;
        Result(int code, String reason) { this.code = code; this.reason = reason; }
        boolean appBlocked() {
            return (code == 401 || code == 403) && "API_KEY_ANDROID_APP_BLOCKED".equals(reason);
        }
        public String toString() { return code + (reason.length() == 0 ? "" : " " + reason); }
    }
    private static Result post(Context context, String endpoint, String key, String body,
                            boolean route, boolean wrongApp) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("X-Android-Package", wrongApp ? "com.example.loki.invalid" : context.getPackageName());
            connection.setRequestProperty("X-Android-Cert", wrongApp ? "0000000000000000000000000000000000000000" : fingerprint(context));
            connection.setRequestProperty("X-Goog-Api-Key", key);
            if (route) connection.setRequestProperty("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String reason = "";
            if (stream != null) {
                try (InputStream input = stream) {
                    if (code >= 400) {
                        java.io.ByteArrayOutputStream bytesOut = new java.io.ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while (bytesOut.size() < 16384 && (length = input.read(buffer)) > 0) bytesOut.write(buffer, 0, length);
                        try {
                            org.json.JSONObject error = new org.json.JSONObject(bytesOut.toString("UTF-8")).optJSONObject("error");
                            org.json.JSONArray details = error == null ? null : error.optJSONArray("details");
                            if (details != null) for (int i = 0; i < details.length(); i++) {
                                String candidate = details.getJSONObject(i).optString("reason", "");
                                // Only machine reason codes; never messages/metadata/URLs.
                                if (candidate.matches("[A-Z_]{3,80}")) { reason = candidate; break; }
                            }
                        } catch (org.json.JSONException ignored) { }
                    }
                }
            }
            return new Result(code, reason);
        } finally { connection.disconnect(); }
    }
    static String fingerprint(Context context) throws Exception {
        Signature[] signatures = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo.getApkContentsSigners();
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(signatures[0].toByteArray());
        StringBuilder result = new StringBuilder();
        for (byte value : hash) result.append(String.format(java.util.Locale.US, "%02X", value & 255));
        return result.toString();
    }
}
