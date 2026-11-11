package com.example.rokidkeyboardbridge;
import org.json.*;
/** Keep the last route visible during refresh, never re-enable cues from reference data. */
final class NavigationDisplayContinuity {
 private JSONObject last;
 JSONObject accept(JSONObject input)throws JSONException {
  if(!input.optBoolean("navigationActive")||input.optBoolean("navigationComplete")||input.optBoolean("suppressed")){last=null;return input;}
  String target=input.optString("routeDestination");
  if(last!=null&&!target.equals(last.optString("routeDestination")))last=null;
  if(input.optBoolean("routeReady")&&!input.optBoolean("routeRefreshing")){last=new JSONObject(input.toString());return input;}
  if(last==null)return input;
  JSONObject out=new JSONObject(input.toString());
  for(String key:new String[]{"instruction","detail","nextDistance","arrival","afterNextInstruction","afterNextDistance","afterNextDuration","totalRemainingDistance","totalRemainingDuration","routeArrival","currentRoad","route","journey","routeMode","routeTime","routeGeneration"})if(last.has(key))out.put(key,last.get(key));
  out.put("referenceOnly",true).put("routeReady",true).put("routeRefreshing",true).put("recoveryMessage","更新待ち・前回の案内");
  return out;
 }
}
