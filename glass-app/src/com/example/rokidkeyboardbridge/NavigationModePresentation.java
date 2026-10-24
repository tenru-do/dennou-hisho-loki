package com.example.rokidkeyboardbridge;
import org.json.*;
public final class NavigationModePresentation {
 public static String effective(String mode,String phase,String access,String egress){if(!"transit".equals(mode))return mode;return phase.equals("FIRST_MILE")?(access.equals("bicycling")?"bicycling":"walking"):phase.equals("LAST_MILE")?(egress.equals("bicycling")?"bicycling":"walking"):"transit";}
 public static String ride(JSONObject journey,String phase){if(journey==null||phase.equals("LAST_MILE"))return "";JSONObject current=journey.optJSONObject("currentLeg");JSONObject chosen=current!=null&&"TRANSIT".equals(current.optString("travelMode"))?current:null;JSONArray rows=journey.optJSONArray("transitStops");for(int i=0;chosen==null&&rows!=null&&i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&"TRANSIT".equals(row.optString("travelMode")))chosen=row;}if(chosen==null)return "行き先：未確認";JSONObject details=chosen.optJSONObject("transitDetails");String head=details==null?"":details.optString("headsign"),line=chosen.optString("lineName");return (line.isEmpty()?"路線：未確認":line)+"\n行き先："+(head.isEmpty()?"未確認":head)+(chosen.optString("arrivalStop").isEmpty()?"":"\n降車予定："+chosen.optString("arrivalStop"));}
 public static String voice(String text,boolean active){if(text==null)return "";String s=text.trim().replaceAll("[\\s　。?!？！]","");if(s.matches("(行き|帰り)のルート(を開いて|を出して|を確認)?"))return s.startsWith("帰り")?"return":"outbound";if(active&&s.matches("もう少し早く教えて|ナビを早めに教えて|案内をもう少し早く"))return "timing";return "";}
 private NavigationModePresentation(){}
}
