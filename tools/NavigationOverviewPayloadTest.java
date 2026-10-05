package com.example.rokidgeminisecretary;
import org.json.*;
public final class NavigationOverviewPayloadTest {
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        JSONArray steps=new JSONArray();
        for(String line:new String[]{"中央線快速","山手線"})steps.put(new JSONObject().put("travelMode","TRANSIT")
            .put("transitDetails",new JSONObject().put("transitLine",new JSONObject().put("name",line))));
        TransitJourney journey=TransitJourney.parse("渋谷駅",new JSONObject().put("polyline",new JSONObject().put("encodedPolyline","valid"))
            .put("legs",new JSONArray().put(new JSONObject().put("steps",steps))),100000);
        JSONObject progress=new JSONObject().put("transitStops",new JSONArray().put(new JSONObject().put("lineName","山手線")))
            .put("finalArrivalTime","2026-10-03T13:35:00Z");
        JSONObject data=NavigationOverviewPayload.create(journey,2520,18400,100000,new JSONArray(),progress);
        check(data.getJSONArray("transitStops").length()==2,"whole trip retains departed lines");
        check(data.getDouble("durationSeconds")==2520&&data.getDouble("distanceMeters")==18400,"whole metrics");
        check(data.getString("arrivalTime").equals("2026-10-03T13:35:00Z"),"scheduled arrival");
        JSONArray maneuvers=new JSONArray().put(new JSONObject().put("instruction","甲州街道を進む"))
            .put(new JSONObject().put("name","中央道")).put(new JSONObject().put("name","中央道"));
        data=NavigationOverviewPayload.create(journey,60,1000,100000,maneuvers,new JSONObject());
        check(data.getJSONArray("roads").toString().equals("[\"甲州街道\",\"中央道\"]"),"explicit roads and dedup");
        check(data.getString("arrivalTime").equals("1970-01-01T00:02:40Z"),"fetched-time ETA fallback");
        data=NavigationOverviewPayload.create(journey,-1,-1,100000,new JSONArray(),new JSONObject());
        check(data.getString("arrivalTime").isEmpty(),"missing duration no fabricated ETA");
        System.out.println("Overview payload PASS: 6 checks");
    }
}
