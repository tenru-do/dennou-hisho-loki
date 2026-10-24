package com.example.rokidgeminisecretary;
import org.json.*;
/** Timetable-aware warnings, never a claim of live service or boarding. */
public final class NavigationCommutePolicy {
 public static int buffer(JSONObject leg){return leg.optInt("parkingMinutes",0)+leg.optInt("gateMinutes",0)+leg.optInt("stationMinutes",0);}
 public static void validate(JSONObject leg)throws Exception{for(String k:new String[]{"parkingMinutes","gateMinutes","stationMinutes"}){Object v=leg.opt(k);if(v!=null&&(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).intValue()||((Number)v).intValue()<0||((Number)v).intValue()>60))throw new Exception("余裕時間は各0〜60分で指定してください");}}
 public static String resumeNotice(int index){return index==0?"最初の区間から開始します。":"前の"+index+"区間は案内しません。乗車・駐輪を済ませ、選んだ出発地点にいることを確認してください。";}
 static long instant(String s){try{return java.time.Instant.parse(s).toEpochMilli();}catch(Exception e){return -1;}}
 public static String warning(JSONObject route,JSONObject leg,long now)throws Exception{
  int margin=buffer(leg);JSONArray legs=route.optJSONArray("legs");if(legs==null)return "乗り継ぎ時刻は未確認です。";
  double walking=0;long previous=now;boolean known=true,seen=false;StringBuilder out=new StringBuilder();
  for(int i=0;i<legs.length();i++){JSONArray steps=legs.getJSONObject(i).optJSONArray("steps");if(steps==null)continue;for(int j=0;j<steps.length();j++){JSONObject step=steps.getJSONObject(j);if(!"TRANSIT".equals(step.optString("travelMode"))){double duration=TransitJourney.duration(step.optString("staticDuration"));if(duration<0)known=false;else walking+=duration;continue;}
   JSONObject d=step.optJSONObject("transitDetails"),st=d==null?null:d.optJSONObject("stopDetails");long departure=st==null?-1:instant(st.optString("departureTime"));
   if(out.length()>0)out.append("\n");if(!known||previous<=0||departure<=0||"navitime".equals(route.optString("lokiProvider")))out.append("乗り継ぎ余裕は未確認（時刻情報不足）。設定 ").append(margin).append("分。");else {long free=(long)Math.floor((departure-previous)/60000.0-walking/60.0);out.append(free<margin?"注意：設定した乗り換え余裕を満たしません。":"時刻表上の乗り換え余裕：").append(free).append("分／設定 ").append(margin).append("分。");}
   seen=true;previous=st==null?-1:instant(st.optString("arrivalTime"));walking=0;known=true;
  }}return seen?out+"\n時刻表上の計算です。遅延・運休・実際の乗車可否は未確認です。":"この区間に公共交通はありません。余裕時間は乗車区間で指定してください。";
 }
 public static int retryIndex(JSONObject active,JSONObject plan,String destination,String mode,String phase)throws Exception{
  if(active==null||!active.optString("id").equals(plan.optString("id"))||active.optInt("revision")!=plan.optInt("revision")||!destination.equals(active.optString("destination"))||!mode.equals(active.optString("mode")))throw new Exception("現在の案内と登録区間が異なります。一覧から選び直してください");
  int index=active.getInt("index");JSONObject leg=plan.getJSONArray("legs").getJSONObject(index);if(!SavedNavigationRoutes.mode(leg).equals("transit")||!phase.matches("FIRST_MILE|BOARDING"))throw new Exception("乗車前の登録区間で利用してください。乗車中は降車後に操作してください");return index;
 }
 private NavigationCommutePolicy(){}
}
