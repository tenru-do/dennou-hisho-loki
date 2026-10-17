package com.example.rokidkeyboardbridge;
import org.json.*;
public class JourneyHudTest {
    public static void main(String[] args) throws Exception {
        JSONArray whole=new JSONArray("[[35,139],[36,140],[37,141]]");
        JSONArray part=new JSONArray("[[36,140],[37,141]]");
        JSONObject leg=new JSONObject().put("travelMode","TRANSIT").put("route",part)
                .put("lineName","テスト線").put("arrivalStop","B駅").put("arrivalTime","2026-09-27T01:00:00Z");
        JSONObject j=new JSONObject().put("version",1).put("id","a").put("finalDestination","目的地")
                .put("positionConfirmed",true).put("wholeRoute",whole).put("currentLeg",leg);
        JSONObject input=new JSONObject().put("journey",j).put("instruction","右折")
                .put("afterNextInstruction","左折").put("nextDistance","100m");
        JSONObject normal=JourneyHud.apply(input,false), overview=JourneyHud.apply(input,true);
        check(normal.getJSONArray("route").length()==2,"current geometry");
        check(overview.getJSONArray("route").length()==3,"whole geometry");
        check(normal.getString("detail").contains("B駅") && normal.getString("detail").contains("10:00"),"arrival");
        check(normal.getString("afterNextInstruction").contains("B駅") && normal.getString("nextDistance").isEmpty(),"next alighting point, no road turns aboard");
        leg.put("durationSeconds",1200).put("remainingSeconds",300).put("stopCount",7);
        j.put("totalRemainingSeconds",1800).put("finalArrivalTime","2026-09-27T01:30:00Z")
                .put("transitStops",new JSONArray().put(new JSONObject().put("lineName","テスト線"))
                        .put(new JSONObject().put("lineName","バス")));
        JSONObject timed=JourneyHud.apply(input,true);
        check(timed.getString("afterNextInstruction").contains("区間7駅") && timed.getString("afterNextInstruction").contains("所要20分"),"segment count and duration");
        check(timed.getString("afterNextDuration").contains("5分"),"next point remaining");
        check(timed.getString("totalRemainingDuration").contains("30分") && timed.getString("routeArrival").contains("10:30"),"final time coexists");
        check(timed.getString("transportSummary").contains("乗換1回") && timed.getString("transportSummary").contains("バス"),"overview transport");
        j.put("walkingFallback",true);
        leg.put("travelMode","WALK").put("kind","WALK");
        JSONObject fallback=JourneyHud.apply(input,false);
        check(fallback.getString("detail").contains("自転車通行可否は未確認"),"fallback safety label");
        check(fallback.getString("instruction").equals("徒歩区間"),"nonempty walk instruction");
        j.put("positionConfirmed",false);
        check(JourneyHud.apply(input,false).getString("instruction").equals("右折"),"preserve guidance with uncertain GPS");
        input.put("source","navigation_sdk");
        check(JourneyHud.apply(input,true)==input,"SDK untouched");
        check(JourneyHud.apply(new JSONObject().put("instruction","legacy"),false).getString("instruction").equals("legacy"),"legacy untouched");
        input.remove("source"); j.remove("currentLeg");
        j.put("phase","TRANSIT_LEG");input.put("routeDestination","降車駅");
        check(JourneyHud.apply(input,false).getString("routeDestination").equals("目的地"),"final facility is not overwritten by intermediate station");
        j.put("waitingToBoard",true);
        check(JourneyHud.apply(input,false).getString("instruction").contains("乗車待ち"),"near platform does not claim boarding");
        j.remove("waitingToBoard");
        for (String mode : new String[]{"walking","bicycling","WALK","BICYCLE","transit","TRANSIT"}) {
            j.put("requestedMode",mode).put("effectiveMode","walking");
            JSONObject waiting=JourneyHud.apply(input,false);
            check(waiting.getBoolean("routeReady") && waiting.getJSONArray("route").length()==3,"GPS waiting geometry " + mode);
            check(JourneyHud.routeFor(j,false).length()==3,"mode switch geometry " + mode);
        }
        System.out.println("JourneyHud checks passed");
    }
    private static void check(boolean value,String label) { if(!value)throw new AssertionError(label); }
}
