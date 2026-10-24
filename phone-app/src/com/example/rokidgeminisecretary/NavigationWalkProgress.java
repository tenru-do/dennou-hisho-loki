package com.example.rokidgeminisecretary;
import org.json.*;
/** Session-local movement evidence. Never uses device-facing direction as walking direction. */
public final class NavigationWalkProgress {
 private String route="",step="";private long last,headingSince,awaySince;private int headingSamples,awaySamples;private double min=Double.MAX_VALUE,anchorLat=Double.NaN,anchorLng=Double.NaN;private boolean headingReady,missed,requested;private double lastDirection=Double.NaN;private long lastDirectionAt;
 public synchronized JSONObject update(String id,String maneuver,long now,long sample,double lat,double lng,double accuracy,double motionBearing,double speed,double routeBearing,double offRoute,double targetDistance)throws Exception{
  if(!id.equals(route)){route=id;step="";last=headingSince=awaySince=0;headingSamples=awaySamples=0;min=Double.MAX_VALUE;anchorLat=anchorLng=Double.NaN;headingReady=missed=requested=false;lastDirection=Double.NaN;lastDirectionAt=0;}
  boolean fresh=NavigationTurnFix.fresh(now,sample,accuracy)&&Double.isFinite(lat)&&Double.isFinite(lng)&&Math.abs(lat)<=90&&Math.abs(lng)<=180;
  if(!fresh)return view(false,Double.NaN);
  double direction=lastDirectionAt>0&&now>=lastDirectionAt&&now-lastDirectionAt<=15000?lastDirection:Double.NaN;
  if(sample>last){
   if(last>0&&sample-last>15000){headingSamples=0;headingSince=0;headingReady=false;lastDirection=Double.NaN;lastDirectionAt=0;anchorLat=anchorLng=Double.NaN;}
   if(accuracy<=20){double observed=Double.NaN;
    if(Double.isFinite(motionBearing)&&motionBearing>=0&&motionBearing<360&&Double.isFinite(speed)&&speed>=0.8&&speed<=3.5)observed=motionBearing;
    else if(Double.isFinite(anchorLat)&&distance(anchorLat,anchorLng,lat,lng)>=Math.max(10,accuracy*2))observed=bearing(anchorLat,anchorLng,lat,lng);
    if(!Double.isFinite(anchorLat)){anchorLat=lat;anchorLng=lng;}
    if(Double.isFinite(observed)){direction=observed;lastDirection=observed;lastDirectionAt=sample;anchorLat=lat;anchorLng=lng;if(headingSamples++==0)headingSince=sample;if(sample-headingSince>=2000)headingReady=true;}
   }
   last=sample;
   if(!maneuver.equals(step)){step=maneuver;min=Double.MAX_VALUE;awaySince=0;awaySamples=0;}
   if(Double.isFinite(targetDistance)&&targetDistance>=0&&Double.isFinite(offRoute)&&offRoute>=0){
    if(accuracy<=20&&min<=30&&targetDistance>=min+Math.max(25,accuracy*2)&&offRoute>Math.max(20,accuracy*1.5)){if(awaySamples++==0)awaySince=sample;if(awaySamples>=2&&sample-awaySince>=3000)missed=true;}
    else{awaySamples=0;awaySince=0;}min=Math.min(min,targetDistance);
   }
  }
  double relative=headingReady&&Double.isFinite(direction)&&Double.isFinite(routeBearing)?((routeBearing-direction+540)%360)-180:Double.NaN;
  return view(true,relative);
 }
 private JSONObject view(boolean fresh,double relative)throws Exception{
  String direction=!fresh?"位置未確認":!headingReady?"出発方向を確認中（移動方向の取得待ち）":!Double.isFinite(relative)?"移動方向の更新待ち":Math.abs(relative)<=30?"↑ 進行方向へ":relative>30&&relative<150?"→ 進行方向から右へ":relative< -30&&relative> -150?"← 進行方向から左へ":"↓ 進行方向と反対側へ";
  return new JSONObject().put("headingConfirmed",fresh&&headingReady&&Double.isFinite(relative)).put("direction",direction).put("missed",missed).put("status",missed?"曲がり角を通り過ぎた可能性があります。経路を確認中":!fresh||!headingReady?direction:"");
 }
 public synchronized boolean takeReroute(){if(!missed||requested)return false;requested=true;return true;}
 private static double distance(double a,double b,double c,double d){double x=Math.sin(Math.toRadians(a-c)/2),y=Math.sin(Math.toRadians(b-d)/2);return 12742000*Math.asin(Math.sqrt(Math.min(1,x*x+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*y*y)));}
 public static double routeBearing(JSONArray points,int nearest){try{JSONArray a=points.getJSONArray(nearest);for(int i=nearest+1;i<Math.min(points.length(),nearest+26);i++){JSONArray b=points.getJSONArray(i);if(distance(a.getDouble(0),a.getDouble(1),b.getDouble(0),b.getDouble(1))>=3)return bearing(a.getDouble(0),a.getDouble(1),b.getDouble(0),b.getDouble(1));}}catch(Exception invalid){}return Double.NaN;}
 public static double bearing(double a,double b,double c,double d){double x=Math.toRadians(d-b),y=Math.sin(x)*Math.cos(Math.toRadians(c)),z=Math.cos(Math.toRadians(a))*Math.sin(Math.toRadians(c))-Math.sin(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.cos(x);return (Math.toDegrees(Math.atan2(y,z))+360)%360;}
}
