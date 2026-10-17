package com.example.rokidgeminisecretary;
import org.json.*;
public final class NavigationRouteChoicesTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static JSONObject ride(String name,String from,String to,int seconds)throws Exception {
  return new JSONObject().put("travelMode","TRANSIT").put("staticDuration",seconds+"s").put("distanceMeters",1000)
   .put("polyline",new JSONObject().put("encodedPolyline",GooglePolyline.encode(java.util.Arrays.asList(new double[]{35,139},new double[]{35.1,139.1}))))
   .put("transitDetails",new JSONObject().put("transitLine",new JSONObject().put("name",name))
    .put("stopDetails",new JSONObject().put("departureStop",new JSONObject().put("name",from)).put("arrivalStop",new JSONObject().put("name",to))));
 }
 static JSONObject route(JSONObject...steps)throws Exception{return new JSONObject().put("duration","1000s").put("legs",new JSONArray().put(new JSONObject().put("steps",new JSONArray(java.util.Arrays.asList(steps)))));}
 public static void main(String[]args)throws Exception {
  NavigationRouteChoices store=new NavigationRouteChoices();
  JSONObject a=route(ride("甲線","A駅","B駅",600)),b=route(ride("乙線","C駅","D駅",800));
  JSONObject response=store.publish("勤務先","transit","35,139",new JSONArray().put(a).put(b),1000);
  String id=response.getJSONArray("choices").getJSONObject(1).getString("id");
  check(store.cached("勤務先","transit","35,139",2000).toString().equals(response.toString()),"both clients get identical IDs and labels");
  JSONObject selected=store.select(id,"勤務先","transit","35,139",2000);
  check(selected.getString("lokiSelectionLabel").startsWith("2."),"selected candidate number retained on phone and wire");
  check(TransitJourney.normalizedTime("2026-10-15T09:10:00").equals("2026-10-15T00:10:00Z"),"provider local departure interpreted in Japan");
  check(NavigationRouteChoices.label(selected,1).contains("乙線"),"second candidate actually used");selected.put("duration","1s");
  check(store.select(id,"勤務先","transit","35,139",2000).getString("duration").equals("1000s"),"selection cannot mutate snapshot");
  for(String[] invalid:new String[][]{{"勤務先","walking","35,139"},{"別の場所","transit","35,139"},{"勤務先","transit","36,140"}}){
   boolean rejected=false;try{store.select(id,invalid[0],invalid[1],invalid[2],2000);}catch(Exception e){rejected=true;}check(rejected,"bound mode destination coordinates");}
  check(store.cached("勤務先","transit","35,139",181001)==null,"expires");
  check(store.cached("勤務先","transit","35,139",999)==null,"clock rollback");
  JSONObject walk=new JSONObject().put("travelMode","WALK").put("staticDuration","50s").put("distanceMeters",10);
  JSONObject whole=route(walk,ride("甲線","A駅","B駅",600),walk,ride("乙線","B駅","C駅",800),walk).put("lokiProvider","navitime");
  JSONObject transit=MultimodalJourney.transitSection(whole);
  check(transit.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").length()==3,"retain transfers, drop access and egress");
  check(transit.getString("lokiProvider").equals("navitime"),"retain selected provider");
  check(GooglePolyline.decode(transit.getJSONObject("polyline").getString("encodedPolyline")).length>=2,"rail geometry survives boarding");
  check(whole.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").length()==5,"source route immutable");
  System.out.println("NavigationRouteChoices PASS shared candidates, selected route, expiry/binding, rail geometry and transfers");
 }
}
