package com.example.rokidgeminisecretary;
import java.util.ArrayList;
final class GooglePolyline {
    static void mergeStepsOrOverview(org.json.JSONObject route) throws org.json.JSONException {
        org.json.JSONObject overview = route.optJSONObject("polyline");
        try { mergeSteps(route); }
        catch (Exception invalidSteps) {
            if (overview == null) throw new org.json.JSONException("Missing route geometry");
            decode(overview.optString("encodedPolyline"));
            route.put("polyline", overview);
        }
    }
    static void mergeSteps(org.json.JSONObject route) throws org.json.JSONException {
        ArrayList<double[]> all = new ArrayList<>();
        org.json.JSONArray legs = route.optJSONArray("legs");
        if (legs == null) return;
        for (int i = 0; i < legs.length(); i++) {
            org.json.JSONArray steps = legs.getJSONObject(i).optJSONArray("steps");
            if (steps == null) return;
            for (int j = 0; j < steps.length(); j++) {
                org.json.JSONObject step = steps.getJSONObject(j);
                org.json.JSONObject line = step.optJSONObject("polyline");
                if ((line == null || line.optString("encodedPolyline").isEmpty())
                        && step.has("distanceMeters") && step.optInt("distanceMeters", -1) == 0) continue;
                if (line == null) return; // Keep API overview rather than fabricate missing geometry.
                double[][] points;
                try { points = decode(line.optString("encodedPolyline")); }
                catch (IllegalArgumentException missing) { return; }
                for (double[] p : points) {
                    double[] last = all.isEmpty() ? null : all.get(all.size()-1);
                    if (last == null || last[0] != p[0] || last[1] != p[1]) all.add(p);
                }
            }
        }
        if (all.size() < 2) return;
        StringBuilder encoded = new StringBuilder();
        long lat = 0, lng = 0;
        for (double[] p : all) {
            long nextLat = Math.round(p[0]*1e5), nextLng = Math.round(p[1]*1e5);
            encodeComponent(encoded, nextLat-lat); encodeComponent(encoded, nextLng-lng);
            lat = nextLat; lng = nextLng;
        }
        route.put("polyline", new org.json.JSONObject().put("encodedPolyline", encoded.toString()));
    }
    private static void encodeComponent(StringBuilder out, long delta) {
        long value = delta < 0 ? ~(delta << 1) : delta << 1;
        while (value >= 32) { out.append((char)((value & 31) + 95)); value >>= 5; }
        out.append((char)(value + 63));
    }
    static double[][] decode(String encoded) {
        ArrayList<double[]> points = new ArrayList<double[]>();
        int[] cursor = {0};
        long lat = 0, lng = 0;
        while (cursor[0] < encoded.length()) {
            lat += component(encoded, cursor);
            lng += component(encoded, cursor);
            if (Math.abs(lat) > 9000000 || Math.abs(lng) > 18000000 || points.size() >= 100000)
                throw new IllegalArgumentException("invalid polyline");
            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        if (points.size() < 2) throw new IllegalArgumentException("empty polyline");
        return points.toArray(new double[points.size()][]);
    }
    private static long component(String s, int[] cursor) {
        long value = 0;
        for (int shift = 0; shift <= 30; shift += 5) {
            if (cursor[0] >= s.length()) throw new IllegalArgumentException("truncated polyline");
            int b = s.charAt(cursor[0]++) - 63;
            if (b < 0 || b > 63) throw new IllegalArgumentException("invalid polyline");
            value |= (long)(b & 31) << shift;
            if (b < 32) return (value & 1) == 0 ? value >> 1 : ~(value >> 1);
        }
        throw new IllegalArgumentException("invalid polyline");
    }
}
