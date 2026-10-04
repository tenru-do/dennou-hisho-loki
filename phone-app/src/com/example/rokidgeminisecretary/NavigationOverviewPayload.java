package com.example.rokidgeminisecretary;
import org.json.*;
import java.util.*;
import java.util.regex.*;

/** Whole-route values are kept distinct from remaining-route guidance. */
final class NavigationOverviewPayload {
    static JSONObject create(TransitJourney journey, double seconds, double meters,
            long fetchedAt, JSONArray maneuvers, JSONObject progress) throws JSONException {
        JSONObject out=new JSONObject().put("durationSeconds",seconds).put("distanceMeters",meters);
        String arrival=progress.optString("finalArrivalTime");
        if(arrival.isEmpty() && Double.isFinite(seconds) && seconds>=0)
            arrival=java.time.Instant.ofEpochMilli(fetchedAt+(long)(seconds*1000)).toString();
        out.put("arrivalTime",arrival);
        JSONArray rides=new JSONArray();
        for(TransitJourney.Leg leg:journey.legs)if("TRANSIT".equals(leg.travelMode))
            rides.put(new JSONObject().put("departureStop",leg.departureStop)
                .put("arrivalStop",leg.arrivalStop).put("lineName",leg.lineName));
        out.put("transitStops",rides);
        LinkedHashSet<String> roads=new LinkedHashSet<>();
        // Only explicit road names; never invent roads from the route geometry.
        Pattern road=Pattern.compile("[\\p{L}\\p{N}・]+(?:街道|自動車道|高速道路|道路)|(?:国道|県道|都道)[0-9０-９]+号|[\\p{L}\\p{N}・]+[道線](?=を|に|経由|方面|$)");
        if(maneuvers!=null)for(int i=0;i<maneuvers.length();i++) {
            JSONObject m=maneuvers.optJSONObject(i);if(m==null)continue;
            String name=m.optString("name").trim();if(!name.isEmpty())roads.add(name);
            Matcher match=road.matcher(m.optString("instruction"));
            while(match.find())roads.add(match.group());
        }
        out.put("roads",new JSONArray(roads));return out;
    }
}
