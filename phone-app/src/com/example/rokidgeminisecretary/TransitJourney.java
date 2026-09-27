package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Phone-only immutable snapshot. A Leg is one API step, not a Routes API leg.
 * Keep the original geometry and transit fields, without persisting location data to disk.
 */
final class TransitJourney {
    final String id, finalDestination, encodedPolyline;
    final long fetchedAt;
    final List<Leg> legs;
    static final class Leg {
        final String travelMode, kind, encodedPolyline, transitDetailsJson;
        final String departureStop, arrivalStop, departureTime, arrivalTime, lineName;
        final String instruction;
        Leg(JSONObject step, String kind) throws JSONException {
            travelMode = step.optString("travelMode", "UNKNOWN");
            JSONObject navigation = step.optJSONObject("navigationInstruction");
            instruction = navigation == null ? "" : navigation.optString("instructions", "");
            this.kind = kind;
            JSONObject geometry = step.optJSONObject("polyline");
            encodedPolyline = geometry == null ? "" : geometry.optString("encodedPolyline", "");
            JSONObject details = step.optJSONObject("transitDetails");
            transitDetailsJson = details == null ? "{}" : details.toString();
            JSONObject stops = details == null ? null : details.optJSONObject("stopDetails");
            departureStop = stopName(stops, "departureStop");
            arrivalStop = stopName(stops, "arrivalStop");
            departureTime = stops == null ? "" : stops.optString("departureTime", "");
            arrivalTime = stops == null ? "" : stops.optString("arrivalTime", "");
            JSONObject line = details == null ? null : details.optJSONObject("transitLine");
            lineName = line == null ? "" : line.optString("name", line.optString("nameShort", ""));
        }
        private static String stopName(JSONObject stops, String key) {
            JSONObject stop = stops == null ? null : stops.optJSONObject(key);
            return stop == null ? "" : stop.optString("name", "");
        }
    }
    private TransitJourney(String destination, String geometry, List<Leg> legs, long now) {
        id = UUID.randomUUID().toString(); finalDestination = destination;
        encodedPolyline = geometry; fetchedAt = now;
        this.legs = Collections.unmodifiableList(legs);
    }
    static TransitJourney parse(String destination, JSONObject route, long now) throws JSONException {
        String geometry = route.getJSONObject("polyline").getString("encodedPolyline");
        if (destination == null || destination.trim().isEmpty() || geometry.isEmpty())
            throw new JSONException("Missing journey identity or geometry");
        List<JSONObject> steps = new ArrayList<JSONObject>();
        JSONArray apiLegs = route.getJSONArray("legs");
        int firstTransit = -1, lastTransit = -1;
        for (int i = 0; i < apiLegs.length(); i++) {
            JSONArray items = apiLegs.getJSONObject(i).getJSONArray("steps");
            for (int j = 0; j < items.length(); j++) {
                JSONObject step = items.getJSONObject(j);
                if ("TRANSIT".equals(step.optString("travelMode"))) {
                    if (firstTransit < 0) firstTransit = steps.size();
                    lastTransit = steps.size();
                }
                steps.add(step);
            }
        }
        if (steps.isEmpty()) throw new JSONException("Missing journey steps");
        List<Leg> legs = new ArrayList<Leg>();
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.get(i);
            boolean transit = "TRANSIT".equals(step.optString("travelMode"));
            String kind = transit ? "RIDE" : firstTransit < 0 ? "WALK"
                    : i < firstTransit ? "ACCESS" : i > lastTransit ? "EGRESS" : "TRANSFER";
            legs.add(new Leg(step, kind));
        }
        return new TransitJourney(destination.trim(), geometry, legs, now);
    }
}
