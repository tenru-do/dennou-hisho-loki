package com.example.rokidgeminisecretary;
import org.json.*;
public final class TransitBoardingStartTest {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static JSONObject location(double a,double b)throws Exception{return new JSONObject().put("latLng",new JSONObject().put("latitude",a).put("longitude",b));}
 static MultimodalJourney trip()throws Exception {
  // Synthetic platform coordinates; this fixture is not an actual timetable/route.
  JSONObject ride=new JSONObject().put("travelMode","TRANSIT").put("staticDuration","900s").put("distanceMeters",2000)
   .put("startLocation",location(35,139)).put("endLocation",location(35.01,139.01))
   .put("polyline",new JSONObject().put("encodedPolyline",GooglePolyline.encode(java.util.Arrays.asList(new double[]{35,139},new double[]{35.005,139.005},new double[]{35.01,139.01}))))
   .put("transitDetails",new JSONObject().put("transitLine",new JSONObject().put("name","テストバス").put("vehicle",new JSONObject().put("type","BUS")))
    .put("stopDetails",new JSONObject().put("departureStop",new JSONObject().put("name","南口バス乗場")).put("arrivalStop",new JSONObject().put("name","テスト降車停留所"))));
  JSONObject route=new JSONObject().put("polyline",ride.getJSONObject("polyline")).put("legs",new JSONArray().put(new JSONObject().put("steps",new JSONArray().put(ride)))).put("lokiSelectionId","chosen-bus");
  MultimodalJourney p=new MultimodalJourney("勤務先施設",new double[]{35.011,139.011},"bicycling","walking");p.stations(route);return p;
 }
 public static void main(String[] args)throws Exception {
  long now=System.currentTimeMillis();MultimodalJourney p=trip();
  check(p.startAtBoardingStop(35,139,10,now,now),"search at selected bus stop starts without a road detour");
  check(p.phase==MultimodalJourney.Phase.BOARDING&&p.mode().equals("transit"),"near platform waits to board, no bike mode");
  check(!p.advance("board"),"not ready until route published");
  JSONObject bus=p.fetchPhase(35,139,(a,b,x,y,m)->{throw new AssertionError("must not route to nearest station or request zero-length road");});
  check(bus.getString("lokiSelectionId").equals("chosen-bus"),"selected bus identity retained");
  check(NavigationRouteChoices.label(bus,0).contains("[バス]"),"bus candidate visibly differs from railway");
  check(bus.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(0).getJSONObject("transitDetails").getJSONObject("transitLine").getString("name").equals("テストバス"),"bus not replaced by train");
  check(p.destination.equals("勤務先施設")&&p.boardingName.equals("南口バス乗場"),"facility and bus platform are distinct");
  p.ready=true;check(p.geofence(35.01,139.01,10,now+1000,now+1000).isEmpty(),"waiting is not automatic board or alight");
  check(p.advance("board")&&p.phase==MultimodalJourney.Phase.TRANSIT_LEG,"boarding requires explicit confirmation");
  check(!trip().startAtBoardingStop(35.01,139.01,10,now,now),"distant platform keeps access leg");
  check(!trip().startAtBoardingStop(35,139,80,now,now),"inaccurate fix cannot skip access");
  check(!trip().startAtBoardingStop(35,139,10,now-31000,now),"stale fix cannot skip access");
  MultimodalJourney walking=trip();walking.ready=true;check(walking.advance("arrive")&&walking.phase==MultimodalJourney.Phase.BOARDING,"geofence arrival only enters waiting state");
  System.out.println("TransitBoardingStart PASS checks="+checks);
 }
}
