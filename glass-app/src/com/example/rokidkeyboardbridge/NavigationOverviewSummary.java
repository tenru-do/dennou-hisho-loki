package com.example.rokidkeyboardbridge;

import org.json.*;
import java.util.*;

/** Immutable whole-trip information; never substitute remaining/next-step values. */
final class NavigationOverviewSummary {
    final String destination, duration, arrival, distance, detailTitle, detail;
    NavigationOverviewSummary(String destination, String duration, String arrival,
            String distance, String detailTitle, String detail) {
        this.destination=destination; this.duration=duration; this.arrival=arrival;
        this.distance=distance; this.detailTitle=detailTitle; this.detail=detail;
    }
    static NavigationOverviewSummary from(JSONObject input) {
        JSONObject journey=input.optJSONObject("journey");
        JSONObject data=journey==null?null:journey.optJSONObject("overviewSummary");
        if(data==null) data=new JSONObject();
        double seconds=data.optDouble("durationSeconds",-1), meters=data.optDouble("distanceMeters",-1);
        String mode=journey==null?input.optString("routeMode"):journey.optString("requestedMode");
        String destination=journey==null?input.optString("routeDestination"):journey.optString("finalDestination");
        String title="経路詳細", detail="全体情報を取得できません";
        if("transit".equalsIgnoreCase(mode)) {
            title="利用路線・乗換";
            JSONArray rides=data.optJSONArray("transitStops");
            if(rides!=null && rides.length()>0) {
                StringBuilder b=new StringBuilder("乗換: "+Math.max(0,rides.length()-1)+"回");
                for(int i=0;i<rides.length();i++) {
                    JSONObject ride=rides.optJSONObject(i); if(ride==null)continue;
                    b.append("\n").append(ride.optString("departureStop","駅名未取得"))
                     .append(" → ").append(ride.optString("lineName","路線未取得"))
                     .append(" → ").append(ride.optString("arrivalStop","駅名未取得"));
                }
                detail=b.toString();
            }
        } else if("driving".equalsIgnoreCase(mode)) {
            title="主な経由道路";
            JSONArray roads=data.optJSONArray("roads");
            if(roads!=null && roads.length()>0) {
                LinkedHashSet<String> names=new LinkedHashSet<>();
                for(int i=0;i<roads.length();i++)if(!roads.optString(i).trim().isEmpty())names.add(roads.optString(i));
                detail=names.isEmpty()?"道路名を取得できません":android.text.TextUtils.join(" / ",names)+" 経由";
            } else detail="道路名を取得できません";
        } else {
            title="移動モード"; detail="walking".equalsIgnoreCase(mode)?"徒歩":"bicycling".equalsIgnoreCase(mode)?"自転車":mode.isEmpty()?"未取得":mode;
        }
        String arrival=JourneyHud.time(data.optString("arrivalTime"));
        return new NavigationOverviewSummary(destination,
            Double.isFinite(seconds)&&seconds>=0?"約"+(long)Math.ceil(seconds/60)+"分":"未取得",
            arrival.isEmpty()?"未取得":arrival,
            Double.isFinite(meters)&&meters>=0?String.format(Locale.JAPAN,"%.1fkm",meters/1000):"未取得",title,detail);
    }
}
