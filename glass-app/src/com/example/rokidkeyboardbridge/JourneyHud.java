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
                .put("routeDestination", journey.optString("phase").isEmpty()
                        ? journey.optString("finalDestination") : input.optString("routeDestination"));
        JSONArray planned = journey.optJSONArray("transitStops");
        if (planned != null && planned.length() > 0) {
            StringBuilder transport = new StringBuilder();
            for (int i = 0; i < planned.length(); i++) {
                if (i > 0) transport.append(" → ");
                transport.append(planned.getJSONObject(i).optString("lineName", "公共交通"));
            }
            out.put("transportSummary", "予定 乗換" + Math.max(0, planned.length() - 1) + "回 " + transport);
        }
        if (leg == null || !journey.optBoolean("positionConfirmed")) {
            // Retain notification guidance while GPS cannot identify the current step.
            if (out.optString("instruction").isEmpty()) out.put("instruction", "現在区間を確認中");
            JSONArray stops = journey.optJSONArray("transitStops");
            if (stops != null && stops.length() > 0) {
                JSONObject next = stops.getJSONObject(0);
                out.put("detail", "予定：" + next.optString("departureStop") + " " + time(next.optString("departureTime"))
                        + " 発 / " + next.optString("lineName") + " → " + next.optString("arrivalStop")
                        + " " + time(next.optString("arrivalTime")) + " 着");
            }
            double scheduledTotal = journey.optDouble("totalRemainingSeconds", -1);
            if (scheduledTotal >= 0) out.put("totalRemainingDuration", minutes(scheduledTotal) + "（予定）")
                    .put("routeArrival", time(journey.optString("finalArrivalTime")) + "予定");
            return out;
        }
        // No mixing Google Maps' independently selected trip with this API journey.
        for (String key : new String[]{"nextDistance", "afterNextInstruction", "afterNextDistance",
                "afterNextDuration", "currentRoad", "arrival", "routeArrival",
                "totalRemainingDistance", "totalRemainingDuration"}) out.put(key, "");
        out.put("routeMode", journey.optString("effectiveMode", "transit"));
        double total = journey.optDouble("totalRemainingSeconds", -1);
        if (total >= 0) out.put("totalRemainingDuration", minutes(total) + "（予定）")
                .put("routeArrival", time(journey.optString("finalArrivalTime")) + "予定");
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
            if ("TRANSIT_LEG".equals(journey.optString("phase"))) {
                JSONObject stops = details == null ? null : details.optJSONObject("stopDetails");
                JSONObject departure = stops == null ? null : stops.optJSONObject("departureStop");
                String platform = departure == null ? "" : departure.optString("platform", "");
                out.put("detail", out.optString("detail") + (platform.isEmpty() ? " / 番線情報未提供" : " / " + platform + "番線"));
            }
            String point = "降車 " + leg.optString("arrivalStop", "停留所");
            int count = leg.optInt("stopCount", -1);
            if (count >= 0) point += "・区間" + count + ("バス".equals(label) ? "停留所" : "駅");
            double remaining = leg.optDouble("remainingSeconds", -1);
            double duration = leg.optDouble("durationSeconds", -1);
            if (duration >= 0) point += "・所要" + minutes(duration);
            out.put("afterNextInstruction", point)
                    .put("afterNextDuration", remaining >= 0 ? minutes(remaining) + "（予定）" : "")
                    .put("afterNextDistance", time(leg.optString("arrivalTime")) + "着予定");
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
            if ("FIRST_MILE".equals(journey.optString("phase"))) out.put("detail", "乗車駅までの区間");
            else if ("LAST_MILE".equals(journey.optString("phase"))) out.put("detail", "最終目的地までの区間");
            if ("ACCESS".equals(kind) || "TRANSFER".equals(kind)) {
                JSONObject next = planned == null ? null : planned.optJSONObject(0);
                if (next != null) out.put("afterNextInstruction", ("TRANSFER".equals(kind) ? "乗換 " : "乗車 ")
                        + next.optString("departureStop") + " " + next.optString("lineName"))
                        .put("afterNextDistance", time(next.optString("departureTime")) + "発予定");
            }
        }
        return out;
    }
    private static String minutes(double seconds) {
        return (long)Math.ceil(seconds / 60.0) + "分";
    }
    static String time(String value) {
        try {
            java.time.Instant instant = java.time.Instant.parse(value);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.JAPAN);
            format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            return format.format(java.util.Date.from(instant));
        } catch (Exception missing) { return "時刻未取得"; }
    }
}
