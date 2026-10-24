package com.example.rokidgeminisecretary;
import org.json.*;
public final class NavigationAlternativePolicy {
 public static String shape(JSONObject r){JSONObject p=r==null?null:r.optJSONObject("polyline");return p==null?"":p.optString("encodedPolyline","");}
 public static JSONArray distinct(JSONArray candidates,JSONObject current)throws Exception{JSONArray result=new JSONArray();String old=shape(current);for(int i=0;i<candidates.length();i++){JSONObject row=candidates.getJSONObject(i);String next=shape(row);if(!old.isEmpty()&&old.equals(next))continue;result.put(row);}return result;}
 public static JSONArray requireDistinct(JSONArray candidates,JSONObject current)throws Exception{JSONArray result=distinct(candidates,current);if(result.length()==0)throw new Exception("navigation_alternative_unavailable");return result;}
 public static String trouble(boolean location,boolean error,boolean updating,boolean ready){return error?"ROUTE":!location?"LOCATION":updating?"UPDATING":!ready?"ROUTE_MISSING":"";}
 private NavigationAlternativePolicy(){}
}
