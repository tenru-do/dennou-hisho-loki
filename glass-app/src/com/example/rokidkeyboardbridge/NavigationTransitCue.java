package com.example.rokidkeyboardbridge;
import org.json.*;
/** Approach to the API's alighting coordinate, never an inferred next station. */
public final class NavigationTransitCue {
 private String key="";private int stage;private long first,last;
 public synchronized String update(JSONObject journey,String phase,double lat,double lng,double accuracy,long sample,long now)throws Exception{
  if(journey==null||"BOARDING".equals(phase)||!journey.optBoolean("positionConfirmed")||sample<=0||now<sample||now-sample>30000||!Double.isFinite(accuracy)||accuracy<0||accuracy>100||!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180){first=last=0;return "";}
  JSONObject leg=journey.optJSONObject("currentLeg");if(leg==null||!"TRANSIT".equals(leg.optString("travelMode"))){first=last=0;return "";}
  String id=journey.optString("id")+"|"+leg.optInt("index",-1)+"|"+leg.optString("arrivalStop");if(!id.equals(key)){key=id;stage=0;first=last=0;}
  String stop=leg.optString("arrivalStop");JSONObject details=leg.optJSONObject("transitDetails"),stops=details==null?null:details.optJSONObject("stopDetails"),target=stops==null?null:stops.optJSONObject("arrivalStop"),location=target==null?null:target.optJSONObject("location"),xy=location==null?null:location.optJSONObject("latLng");
  if(stop.isEmpty()||xy==null)return "";double a=xy.optDouble("latitude",Double.NaN),b=xy.optDouble("longitude",Double.NaN);if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180)return "";
  if(sample<=last)return "";if(last>0&&sample-last>15000)first=0;last=sample;if(first==0)first=sample;
  double metres=distance(lat,lng,a,b);if(metres>800||sample-first<3000)return "";int next=metres<=250?2:1;if(next<=stage)return "";stage=next;
  String message="降車予定の"+stop+"に近づいています。"+(next==2?"降りる準備をしてください。":"車内の案内も確認してください。");
  JSONArray remaining=journey.optJSONArray("transitStops");if(remaining!=null&&remaining.length()>1){String line=remaining.getJSONObject(1).optString("lineName");if(!line.isEmpty())message+="その後は"+line+"への乗り換え予定です。";}return message;
 }
 private static double distance(double a,double b,double c,double d){double x=Math.sin(Math.toRadians(a-c)/2),y=Math.sin(Math.toRadians(b-d)/2);return 12742000*Math.asin(Math.sqrt(Math.min(1,x*x+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*y*y)));}
}
