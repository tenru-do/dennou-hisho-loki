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
        check(normal.getString("afterNextInstruction").isEmpty() && normal.getString("nextDistance").isEmpty(),"no turns aboard");
        j.put("walkingFallback",true);
        leg.put("travelMode","WALK").put("kind","WALK");
        JSONObject fallback=JourneyHud.apply(input,false);
        check(fallback.getString("detail").contains("自転車通行可否は未確認"),"fallback safety label");
        check(fallback.getString("instruction").equals("徒歩区間"),"nonempty walk instruction");
        j.put("positionConfirmed",false);
        check(JourneyHud.apply(input,false).getString("instruction").contains("確認中"),"uncertain GPS");
        input.put("source","navigation_sdk");
        check(JourneyHud.apply(input,true)==input,"SDK untouched");
        check(JourneyHud.apply(new JSONObject().put("instruction","legacy"),false).getString("instruction").equals("legacy"),"legacy untouched");
        System.out.println("JourneyHud: 9 checks passed");
    }
    private static void check(boolean value,String label) { if(!value)throw new AssertionError(label); }
}
