package com.example.rokidkeyboardbridge;

import org.json.*;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Optional v1 extension. SDK/legacy messages stay untouched. */
final class JourneyHud {
    static JSONArray routeFor(JSONObject journey, boolean overview) {
        String mode = journey.optString("requestedMode", "");
        boolean roadJourney = "walking".equals(mode) || "bicycling".equals(mode)
                || "WALK".equals(mode) || "BICYCLE".equals(mode)
                || "transit".equalsIgnoreCase(mode);
        JSONObject leg = journey.optJSONObject("currentLeg");
        // Walking/cycling use the complete valid road geometry even before GPS selects a step.
        JSONArray route = overview || roadJourney ? journey.optJSONArray("wholeRoute")
                : leg == null ? null : leg.optJSONArray("route");
        return route == null ? new JSONArray() : route;
    }
    static JSONObject apply(JSONObject input, boolean overview) throws JSONException {
        if ("transit_access".equals(input.optString("routeTargetKind"))) {
            JSONObject access = new JSONObject(input.toString());
            String duration = access.optString("accessDuration", "");
            if (!duration.isEmpty()) access.put("afterNextInstruction", duration);
            if (overview && access.optBoolean("routeReady"))
                access.put("instruction", "駅までの区間のみ：" + access.optString("instruction"));
            return access;
        }
        JSONObject journey = input.optJSONObject("journey");
        if ("navigation_sdk".equals(input.optString("source")) || journey == null
                || journey.optInt("version") != 1 || journey.optString("id").isEmpty()) return input;
        JSONObject out = new JSONObject(input.toString());
        String fallbackNote = journey.optBoolean("walkingFallback")
                ? "徒歩経路で代替・自転車通行可否は未確認。押し歩きを含みます。 " : "";
        JSONObject leg = journey.optJSONObject("currentLeg");
        JSONArray selected = routeFor(journey, overview);
        out.put("route", selected).put("routeReady", selected.length() >= 2)
                .put("routeDestination", journey.optString("finalDestination"));
        if (leg == null || !journey.optBoolean("positionConfirmed")) {
            // Retain notification guidance while GPS cannot identify the current step.
            if (out.optString("instruction").isEmpty()) out.put("instruction", "現在区間を確認中");
            JSONArray stops = journey.optJSONArray("transitStops");
            if (stops != null && stops.length() > 0 && out.optString("detail").isEmpty()) {
                JSONObject next = stops.getJSONObject(0);
                out.put("detail", "予定：" + next.optString("departureStop") + " " + time(next.optString("departureTime"))
                        + " 発 / " + next.optString("lineName") + " → " + next.optString("arrivalStop")
                        + " " + time(next.optString("arrivalTime")) + " 着");
            }
            return out;
        }
        // No mixing Google Maps' independently selected trip with this API journey.
        for (String key : new String[]{"nextDistance", "afterNextInstruction", "afterNextDistance",
                "afterNextDuration", "currentRoad", "arrival", "routeArrival",
                "totalRemainingDistance", "totalRemainingDuration"}) out.put(key, "");
        out.put("routeMode", journey.optString("effectiveMode", "transit"));
        if (leg == null || !journey.optBoolean("positionConfirmed")) {
            out.put("instruction", "現在区間を確認中").put("detail", fallbackNote + "GPSによる区間推定を待っています");
            return out;
        }
        if ("TRANSIT".equals(leg.optString("travelMode"))) {
            JSONObject details = leg.optJSONObject("transitDetails");
            JSONObject line = details == null ? null : details.optJSONObject("transitLine");
            JSONObject vehicle = line == null ? null : line.optJSONObject("vehicle");
            String type = vehicle == null ? "" : vehicle.optString("type");
            String label = "BUS".equals(type) || "INTERCITY_BUS".equals(type) ? "バス" : "公共交通";
            out.put("instruction", label + "：" + leg.optString("lineName", ""));
            out.put("detail", leg.optString("departureStop", "") + " " + time(leg.optString("departureTime"))
                    + " 発 → 降車 " + leg.optString("arrivalStop", "")
                    + " / " + time(leg.optString("arrivalTime")) + " 着予定（区間推定）");
        } else {
            String mode = "BICYCLE".equals(leg.optString("travelMode")) ? "bicycling" : "walking";
            String instruction = leg.optString("instruction", "").trim();
            if (instruction.isEmpty()) instruction = "bicycling".equals(mode) ? "自転車区間" : "徒歩区間";
            out.put("routeMode", mode).put("instruction", instruction);
            String kind = leg.optString("kind");
            out.put("detail", fallbackNote + ("TRANSFER".equals(kind) ? "乗換の徒歩区間"
                    : "EGRESS".equals(kind) ? "目的地までの徒歩区間"
                    : "ACCESS".equals(kind) ? "駅・停留所までの区間"
                    : "bicycling".equals(mode) ? "目的地までの自転車区間" : "目的地までの徒歩区間"));
        }
        return out;
    }
    private static String time(String value) {
        try {
            java.time.Instant instant = java.time.Instant.parse(value);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.JAPAN);
            format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            return format.format(java.util.Date.from(instant));
        } catch (Exception missing) { return "時刻未取得"; }
    }
}
