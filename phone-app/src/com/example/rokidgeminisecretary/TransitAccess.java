package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONObject;

/** One immutable final target per explicitly started journey. Never fed by notifications. */
final class TransitAccess {
    final String destination, coordinates, mode;
    volatile String station = "", stationCoordinates = "", error = "";
    volatile boolean fetching = true;
    TransitAccess(String destination, String coordinates, String mode) {
        this.destination = destination;
        this.coordinates = coordinates;
        this.mode = "bicycling".equals(mode) ? mode : "walking";
    }
    static JSONObject stationRequest(double lat, double lng) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180)
            throw new Exception("google_current_location_unavailable");
        JSONObject center = new JSONObject().put("latitude", lat).put("longitude", lng);
        return new JSONObject().put("includedTypes", new JSONArray().put("train_station").put("subway_station"))
                .put("rankPreference", "DISTANCE").put("maxResultCount", 1).put("languageCode", "ja")
                .put("locationRestriction", new JSONObject().put("circle", new JSONObject().put("center", center).put("radius", 5000.0)));
    }
    String status() {
        if (fetching) return "最寄り駅候補と駅までの経路を検索中";
        if (error.equals("google_current_location_unavailable")) return "現在地未取得・GPSを確認して再開始してください";
        if (error.equals("google_station_not_found")) return "5km以内に駅候補なし";
        if (error.equals("google_route_not_found")) return "駅までの経路なし";
        if (error.equals("google_maps_not_verified")) return "Google Maps API設定を確認してください";
        if (!error.isEmpty()) return "駅検索／経路取得失敗（" + error + "）";
        return "駅までの区間";
    }
    JSONObject apply(JSONObject out, JSONArray points, String effectiveMode, double seconds) throws Exception {
        boolean ready = points != null && points.length() >= 2 && error.isEmpty();
        out.remove("journey");
        for (String key : new String[]{"arrival", "routeArrival", "totalRemainingDuration", "totalRemainingDistance",
                "afterNextInstruction", "afterNextDistance", "afterNextDuration", "nextDistance"}) out.put(key, "");
        String transport = "bicycling".equals(effectiveMode) ? "自転車" : "徒歩";
        boolean fallback = ready && !mode.equals(effectiveMode);
        out.put("ok", true).put("navigationActive", true).put("time", System.currentTimeMillis())
                .put("destination", destination).put("finalDestinationCoordinates", coordinates)
                .put("routeDestination", station).put("routeTargetKind", "transit_access")
                .put("routeMode", effectiveMode).put("travelMode", "transit").put("accessMode", mode)
                .put("walkingFallback", fallback).put("mapProvider", "google")
                .put("routeReady", ready).put("route", ready ? points : new JSONArray())
                .put("routeStatus", ready ? "ready" : fetching ? "fetching" : "request_failed")
                .put("routeError", error).put("routeRefreshing", fetching)
                .put("instruction", ready ? station + "（自動候補）まで" + transport + (fallback ? "（自転車経路なし）" : "") : status())
                .put("detail", "最終: " + destination + " / 駅は自動候補・乗換はGoogleマップ")
                .put("routeSource", "駅までのみ・自動選定候補（推奨乗車駅とは限りません）")
                .put("accessDuration", ready && seconds > 0 ? "駅まで約" + (int)Math.ceil(seconds / 60) + "分（取得時）" : "");
        return out;
    }
}
