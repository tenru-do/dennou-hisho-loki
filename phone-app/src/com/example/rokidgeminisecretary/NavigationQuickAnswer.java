package com.example.rokidgeminisecretary;
import org.json.*;
/** Answers only from the current local navigation snapshot; never guesses stop counts. */
public final class NavigationQuickAnswer {
 public static String answer(JSONObject p,String kind,long now){if(p==null||!p.optBoolean("navigationActive")||p.optBoolean("suppressed"))return "案内中の経路がありません。";long at=p.optLong("quickAt");if(at<=0||now<at||now-at>30000)return "ナビ情報が古くなっています。接続と更新を確認してください。";
  if(p.optBoolean("guidancePaused"))return "ナビは一時停止中です。再開する区間を確認してください。";
  if(p.optBoolean("routeRefreshing"))return "経路を更新中です。古い方向の案内はお待ちください。";
  JSONObject journey=p.optJSONObject("journey"),current=journey==null?null:journey.optJSONObject("currentLeg");JSONArray stops=journey==null?null:journey.optJSONArray("transitStops");String stop=current!=null&&"TRANSIT".equals(current.optString("travelMode"))?current.optString("arrivalStop"):stops!=null&&stops.length()>0?stops.optJSONObject(0).optString("arrivalStop"):"";
  String age="";long fetched=journey==null?0:journey.optLong("fetchedAt");if(fetched<=0||now<fetched||now-fetched>300000)age="取得済みの予定です。運行状況は未確認です。";
  if((kind.equals("alight")||kind.equals("stops"))&&"LAST_MILE".equals(p.optString("phase")))return "降車後の区間です。次は目的地への移動です。";
  if(kind.equals("alight"))return stop.isEmpty()?"降車地点の情報がありません。区間と乗車状態を確認してください。":"予定の降車地点は"+stop+"です。"+age;
  if(kind.equals("stops"))return "残り駅数は取得できていません。"+(stop.isEmpty()?"乗車区間を確認してください。":"予定の降車地点は"+stop+"です。")+age;
  if(!kind.equals("next"))return "確認する内容を選んでください。";
  if(!p.optBoolean("guidancePositionConfirmed"))return "現在地を確認中です。方向の案内は更新をお待ちください。";if("LAST_MILE".equals(p.optString("phase")))return "降車後の区間です。"+p.optString("instruction","目的地への道順を確認してください。");if("BOARDING".equals(p.optString("phase")))return "乗車地点で便と行き先を確認してください。乗車後に乗車確認を操作してください。";
  if(current!=null&&"TRANSIT".equals(current.optString("travelMode")))return stop.isEmpty()?"降車地点を確認してください。":"予定の降車地点は"+stop+"です。"+age;
  JSONObject walk=p.optJSONObject("walkProgress");if(walk!=null&&(!walk.optBoolean("headingConfirmed")||walk.optBoolean("missed")))return "移動方向と経路を確認中です。";String instruction=p.optString("instruction");return instruction.isEmpty()?"次の案内を取得中です。":instruction+(p.optString("nextDistance").isEmpty()?"":"、"+p.optString("nextDistance"));
 }
 private NavigationQuickAnswer(){}
}
