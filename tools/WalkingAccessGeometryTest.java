package com.example.rokidgeminisecretary;
import org.json.*;
import java.util.*;
public final class WalkingAccessGeometryTest {
    static int checks;
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static JSONObject point(double a,double b)throws Exception{return new JSONObject().put("latitude",a).put("longitude",b);}
    static JSONObject route(JSONArray steps,String encoded)throws Exception{return new JSONObject().put("legs",new JSONArray().put(new JSONObject().put("steps",steps))).put("polyline",new JSONObject().put("encodedPolyline",encoded)).put("duration","600s").put("distanceMeters",850);}
    public static void main(String[] args)throws Exception {
        String road=GooglePolyline.encode(Arrays.asList(new double[]{35,139},new double[]{35,139.002},new double[]{35.003,139.002},new double[]{35.003,139.004}));
        JSONObject one=new JSONObject().put("travelMode","WALK").put("startLocation",point(35,139)).put("endLocation",point(35.003,139.004)).put("polyline",new JSONObject().put("encodedPolyline","_wemE_}jyY"));
        JSONObject walking=route(new JSONArray().put(one),road);
        JSONArray normalized=WalkingAccessGeometry.roadSteps(walking,"WALK");
        check(GooglePolyline.decode(normalized.getJSONObject(0).getJSONObject("polyline").getString("encodedPolyline")).length==4,"road overview survives single-point steps");
        check(!normalized.getJSONObject(0).getBoolean("lokiGeometryEstimated"),"not marked straight approximation");
        JSONObject train=new JSONObject().put("travelMode","TRANSIT").put("startLocation",point(35.003,139.004)).put("endLocation",point(35.02,139.1));
        JSONObject trip=route(new JSONArray().put(one).put(train),road);final int[] requests={0};
        WalkingAccessGeometry.repairLeading(trip,(a,b,c,d,m)->{requests[0]++;check(m.equals("walking"),"walking request");return walking;});
        check(requests[0]==1,"one repair request");GooglePolyline.mergeSteps(trip);
        JSONArray segments=trip.getJSONArray("lokiRouteSegments");
        check(segments.length()==2,"walking then rail indexes");check(segments.getJSONObject(0).getJSONArray("points").length()==4,"walking corners preserved");
        check(segments.getJSONObject(1).getBoolean("estimated"),"missing rail shape stays explicitly estimated");
        JSONArray walk=segments.getJSONObject(0).getJSONArray("points"),rail=segments.getJSONObject(1).getJSONArray("points");
        check(walk.getJSONArray(walk.length()-1).toString().equals(rail.getJSONArray(0).toString()),"station connection");
        JSONObject legacy=new JSONObject().put("travel_mode","WALKING").put("polyline",new JSONObject().put("points",road))
            .put("start_location",new JSONObject().put("lat",35).put("lng",139)).put("end_location",new JSONObject().put("lat",35.003).put("lng",139.004))
            .put("html_instructions","<b>駅</b>まで歩く");
        trip=route(new JSONArray().put(legacy).put(train),road);GooglePolyline.mergeSteps(trip);
        check(trip.getJSONArray("lokiRouteSegments").getJSONObject(0).getJSONArray("points").length()==4,"legacy points decoded");
        check(legacy.getString("travelMode").equals("WALK"),"legacy walking mode normalized");
        check(legacy.getJSONObject("navigationInstruction").getString("instructions").equals("駅まで歩く"),"legacy guidance retained");
        requests[0]=0;WalkingAccessGeometry.repairLeading(trip,(a,b,c,d,m)->{requests[0]++;return walking;});check(requests[0]==0,"valid walking geometry does not refetch");
        JSONArray sections=new JSONArray()
            .put(new JSONObject().put("type","point").put("name","現在地").put("coord",new JSONObject().put("lat",35).put("lon",139)))
            .put(new JSONObject().put("type","move").put("move","walk").put("time",10))
            .put(new JSONObject().put("type","point").put("name","乗車駅").put("coord",new JSONObject().put("lat",35.003).put("lon",139.004)))
            .put(new JSONObject().put("type","move").put("move","local_train").put("line_name","テスト線").put("time",15))
            .put(new JSONObject().put("type","point").put("name","降車駅").put("coord",new JSONObject().put("lat",35.02).put("lon",139.1)));
        JSONObject navitime=NavitimeRouteAdapter.convert(new JSONObject().put("items",new JSONArray().put(new JSONObject().put("sections",sections))),
            (a,b,c,d,m)->walking);
        JSONArray navSteps=navitime.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
        check(GooglePolyline.decode(navSteps.getJSONObject(0).getJSONObject("polyline").getString("encodedPolyline")).length==4,"NAVITIME preserves road overview");
        check(!navSteps.getJSONObject(0).getBoolean("lokiGeometryEstimated"),"NAVITIME walking not straight fallback");
        check(navSteps.getJSONObject(1).getString("travelMode").equals("TRANSIT"),"NAVITIME rail follows walking");
        System.out.println("WalkingAccessGeometry PASS checks="+checks);
    }
}
