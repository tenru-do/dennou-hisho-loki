package com.example.rokidgeminisecretary;
import org.json.*;
public final class NavigationTransitHeading {
 public static String text(JSONObject journey,String phase){if(journey==null||phase.equals("LAST_MILE"))return "";JSONObject leg=journey.optJSONObject("currentLeg");if(leg==null||!leg.optString("travelMode").equals("TRANSIT")){leg=null;JSONArray stops=journey.optJSONArray("transitStops");if(stops!=null&&stops.length()>0)leg=stops.optJSONObject(0);}if(leg==null)return "";JSONObject details=leg.optJSONObject("transitDetails");String head=details==null?"":details.optString("headsign");return "路線："+(leg.optString("lineName").isEmpty()?"未確認":leg.optString("lineName"))+"\n行き先："+(head.isEmpty()?"未確認":head)+"\n降車予定："+(leg.optString("arrivalStop").isEmpty()?"未確認":leg.optString("arrivalStop"));}
 public static String linked(JSONObject leg,JSONObject note,boolean near,boolean beginning){if(leg.optString("landmarkId").isEmpty()||!near&&!beginning)return "";if(note==null)return "関連メモを確認できません。スマホで関連付けを確認してください。";String text="利用者メモ："+note.optString("name")+"／"+note.optString("note");return text.substring(0,Math.min(180,text.length()))+"（現地状況は未確認）";}
 private NavigationTransitHeading(){}
}
