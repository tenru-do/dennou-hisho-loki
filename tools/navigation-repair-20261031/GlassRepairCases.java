package com.example.rokidkeyboardbridge;
import org.json.*;
public final class GlassRepairCases {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static JSONObject manualHud(JSONObject journey)throws Exception{return JourneyHud.apply(new JSONObject().put("journey",journey).put("time",100),false);}
 public static void main(String[] args)throws Exception {
  NavigationDisplayContinuity c=new NavigationDisplayContinuity();JSONObject ready=new JSONObject().put("navigationActive",true).put("routeDestination","fixture A").put("routeReady",true).put("instruction","bus fixture").put("route",new JSONArray().put(new JSONArray().put(35).put(139)).put(new JSONArray().put(36).put(139))).put("locationTime",10);
  c.accept(ready);JSONObject pending=new JSONObject().put("navigationActive",true).put("routeDestination","fixture A").put("routeReady",false).put("locationTime",20);JSONObject ref=c.accept(pending);ok(ref.getBoolean("referenceOnly")&&ref.getString("instruction").equals("bus fixture"),"refresh keeps last instruction");ok(ref.getLong("locationTime")==20,"new fix never replaced by old fix");ok(ref.getBoolean("routeRefreshing"),"reference not fresh guidance");ok(!c.accept(new JSONObject(pending.toString()).put("routeDestination","fixture B")).has("referenceOnly"),"different destination never inherits");
  c.accept(ready);ok(!c.accept(new JSONObject().put("navigationActive",false)).has("referenceOnly"),"stop clears reference");ok(!c.accept(pending).has("referenceOnly"),"stopped route never resurrects");
  JSONObject summary=new JSONObject().put("wholeRoute",new JSONArray().put(new JSONArray().put(1).put(2)).put(new JSONArray().put(3).put(4)));JSONObject journey=new JSONObject().put("overviewSummary",summary).put("wholeRoute",new JSONArray()).put("requestedMode","transit");ok(JourneyHud.routeFor(journey,true).length()==2,"overview uses entire preserved route");
  String[] menu=NavigationEntryPolicy.menu(true);ok(java.util.Arrays.asList(menu).contains("乗りました（電車・バスを選択）"),"boarding on first navigation menu");
  System.out.println("GlassRepairCases PASS "+n);
 }
}
