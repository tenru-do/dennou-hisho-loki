package com.example.rokidgeminisecretary;

import org.json.*;
import java.util.ArrayList;
import java.util.List;

/** GPS-based estimate, not proof of boarding. Never advance on timetable alone. */
final class JourneyProgress {
    private final TransitJourney journey;
    private final List<double[][]> geometry = new ArrayList<double[][]>();
    private int current = -1, candidate = -1;
    private long candidateSince;
    JourneyProgress(TransitJourney journey) {
        this.journey = journey;
        for (TransitJourney.Leg leg : journey.legs) {
            double[][] points;
            try { points = GooglePolyline.decode(leg.encodedPolyline); }
            catch (Exception missing) { points = new double[0][]; }
            geometry.add(points);
        }
    }
    synchronized JSONObject snapshot(double lat, double lng, double accuracy, long sampleTime, long now,
            JSONArray wholeRoute) throws JSONException {
        boolean fresh = Double.isFinite(lat) && Double.isFinite(lng) && accuracy >= 0 && accuracy <= 100
                && sampleTime > 0 && now - sampleTime >= 0 && now - sampleTime <= 30000;
        int best = -1; double distance = Double.MAX_VALUE;
        if (fresh) {
            for (int i = Math.max(0, current); i < geometry.size(); i++) {
                double d = distanceTo(geometry.get(i), lat, lng);
                if (d < distance) { distance = d; best = i; }
            }
        }
        boolean matched = best >= 0 && distance <= Math.max(35, accuracy * 2);
        if (matched && current >= 0 && best != current
                && distanceTo(geometry.get(current), lat, lng) <= distance + 20) best = current;
        if (matched && best != current) {
            if (candidate != best) { candidate = best; candidateSince = sampleTime; }
            // Require newer GPS evidence over at least 5 seconds; repeated HUD polls don't count.
            if (sampleTime - candidateSince >= 5000) { current = best; candidate = -1; }
        } else { candidate = -1; }
        boolean confident = matched && current == best;
        JSONObject out = new JSONObject().put("version", 1).put("id", journey.id)
                .put("finalDestination", journey.finalDestination).put("wholeRoute", wholeRoute)
                .put("fetchedAt", journey.fetchedAt).put("legCount", journey.legs.size())
                .put("currentLegIndex", current).put("positionConfirmed", confident)
                .put("selectionSource", "gps_estimate");
        JSONArray segments=new JSONArray();
        for(int i=0;i<geometry.size();i++)if(geometry.get(i).length>=2)segments.put(new JSONObject().put("index",i).put("travelMode",journey.legs.get(i).travelMode).put("estimated",journey.legs.get(i).geometryEstimated).put("points",pointsJson(geometry.get(i))));
        out.put("routeSegments",segments);
        JSONArray transitStops = new JSONArray();
        for (int i = Math.max(0, current); i < journey.legs.size(); i++) {
            TransitJourney.Leg next = journey.legs.get(i);
            if ("TRANSIT".equals(next.travelMode)) transitStops.put(new JSONObject()
                    .put("departureStop", next.departureStop).put("arrivalStop", next.arrivalStop)
                    .put("departureTime", next.departureTime).put("arrivalTime", next.arrivalTime)
                    .put("lineName", next.lineName).put("stopCount",next.stopCount).put("durationSeconds",next.durationSeconds));
        }
        out.put("transitStops", transitStops);
        long finalArrival = -1;
        double egressSeconds = 0;
        for (int i = journey.legs.size() - 1; i >= 0; i--) {
            TransitJourney.Leg tail = journey.legs.get(i);
            if ("TRANSIT".equals(tail.travelMode)) {
                long arrival = instant(tail.arrivalTime);
                if (arrival > 0 && egressSeconds >= 0) finalArrival = arrival + (long)(egressSeconds * 1000);
                break;
            }
            if (tail.durationSeconds < 0) egressSeconds = -1;
            else if (egressSeconds >= 0) egressSeconds += tail.durationSeconds;
        }
        if (finalArrival > now) out.put("finalArrivalTime", java.time.Instant.ofEpochMilli(finalArrival).toString())
                .put("totalRemainingSeconds", (finalArrival - now) / 1000.0);
        if (current >= 0) {
            TransitJourney.Leg leg = journey.legs.get(current);
            out.put("currentLeg", new JSONObject().put("index", current).put("kind", leg.kind)
                    .put("travelMode", leg.travelMode).put("route", pointsJson(geometry.get(current)))
                    .put("instruction", leg.instruction).put("departureStop", leg.departureStop)
                    .put("arrivalStop", leg.arrivalStop).put("departureTime", leg.departureTime)
                    .put("arrivalTime", leg.arrivalTime).put("lineName", leg.lineName)
                    .put("durationSeconds", leg.durationSeconds).put("stopCount", leg.stopCount)
                    .put("transitDetails", new JSONObject(leg.transitDetailsJson)));
            JSONObject currentLeg = out.getJSONObject("currentLeg");
            long scheduled = instant(leg.arrivalTime);
            if (confident && scheduled > now) currentLeg.put("remainingSeconds", (scheduled - now) / 1000.0);
        }
        return out;
    }
    private static long instant(String text) {
        try { return java.time.Instant.parse(text).toEpochMilli(); }
        catch (Exception missing) { return -1; }
    }
    private static JSONArray pointsJson(double[][] points) throws JSONException {
        JSONArray out = new JSONArray();
        for (int i : RouteGeometry.indices(points, 4096)) out.put(new JSONArray().put(points[i][0]).put(points[i][1]));
        return out;
    }
    private static double distanceTo(double[][] points, double lat, double lng) {
        double best = Double.MAX_VALUE, scale = Math.cos(Math.toRadians(lat));
        for (int i = 1; i < points.length; i++) {
            double ax = (points[i-1][1]-lng)*111320*scale, ay=(points[i-1][0]-lat)*111320;
            double bx = (points[i][1]-lng)*111320*scale, by=(points[i][0]-lat)*111320;
            double dx=bx-ax, dy=by-ay, length=dx*dx+dy*dy;
            double t=length==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/length));
            best=Math.min(best,Math.hypot(ax+t*dx,ay+t*dy));
        }
        return best;
    }
}
