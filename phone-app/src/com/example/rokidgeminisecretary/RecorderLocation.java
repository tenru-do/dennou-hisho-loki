package com.example.rokidgeminisecretary;
import android.content.*;import android.location.*;import org.json.*;
final class RecorderLocation {
 static JSONObject current(Context c)throws Exception{JSONObject result=new JSONObject().put("available",false);
  if(c.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED && c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return result;
  LocationManager manager=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);Location best=null;
  for(String provider:manager.getProviders(true)){try{Location point=manager.getLastKnownLocation(provider);if(point==null||!point.hasAccuracy()||point.getAccuracy()>500)continue;long age=android.os.SystemClock.elapsedRealtimeNanos()-point.getElapsedRealtimeNanos();if(age<0||age>90000000000L)continue;if(best==null||point.getElapsedRealtimeNanos()>best.getElapsedRealtimeNanos())best=point;}catch(SecurityException denied){}}
  if(best==null)return result;return result.put("available",true).put("latitude",best.getLatitude()).put("longitude",best.getLongitude()).put("accuracyMeters",best.getAccuracy()).put("locationAt",best.getTime()).put("source","phone");
 }
}
