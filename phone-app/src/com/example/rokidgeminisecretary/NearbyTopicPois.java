package com.example.rokidgeminisecretary;
import android.content.*;
import android.util.Log;
import org.json.*;

/** Places content stays in memory. Official local landmarks work without an API key. */
final class NearbyTopicPois {
    private static volatile JSONArray live=new JSONArray();
    private static volatile long fetchedAt;
    private static volatile double fetchedLat, fetchedLon;
    private static double[] location(Context context) {
        SharedPreferences p=context.getSharedPreferences("phone_secretary",0);
        if(System.currentTimeMillis()-p.getLong("context_location_time",0)>86400000L)return null;
        try {double lat=Double.parseDouble(p.getString("context_lat","")),lon=Double.parseDouble(p.getString("context_lon",""));
            return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180?new double[]{lat,lon}:null;
        }catch(Exception e){return null;}
    }
    static void refresh(Context context) {
        double[] point=location(context);
        if(point==null){Log.i("LokiTopicData","TOPIC/poi rawResponseCount=0 status=no_location");return;}
        if(System.currentTimeMillis()-fetchedAt<21600000L && LocalPoiCatalog.distance(point[0],point[1],fetchedLat,fetchedLon)<1000)return;
        fetchedAt=System.currentTimeMillis();fetchedLat=point[0];fetchedLon=point[1];live=new JSONArray();
        if(!GoogleNavigationClient.enabled(context)){Log.i("LokiTopicData","TOPIC/poi api=GooglePlaces rawResponseCount=0 status=not_configured");return;}
        try {
            JSONArray raw=GoogleNavigationClient.nearbyTopicPois(context,point[0],point[1]);
            JSONArray found=new JSONArray();
            for(int i=0;i<raw.length();i++) {
                JSONObject place=raw.getJSONObject(i),loc=place.optJSONObject("location"),display=place.optJSONObject("displayName");
                if(loc==null||display==null)continue;
                double lat=loc.optDouble("latitude",Double.NaN),lon=loc.optDouble("longitude",Double.NaN);
                String name=TopicTextPolicy.limit(display.optString("text"),60);
                if(!name.isEmpty() && Double.isFinite(lat)&&Double.isFinite(lon)
                        && LocalPoiCatalog.distance(point[0],point[1],lat,lon)<=LocalPoiCatalog.RADIUS_METERS)
                    found.put(new JSONObject().put("title",name).put("source","Google Maps")
                            .put("latitude",lat).put("longitude",lon).put("poi",true));
            }
            live=found;
        }catch(Exception e){Log.w("LokiTopicData","TOPIC/poi rawResponseCount=0 status=api_failed reason="+e.getClass().getSimpleName());}
    }
    static JSONArray cached(Context context) throws Exception {
        double[] point=location(context);JSONArray result=new JSONArray();
        JSONArray snapshot=live;
        if(point==null)return result;
        if(System.currentTimeMillis()-fetchedAt<21600000L) for(int i=0;i<snapshot.length();i++) {
            JSONObject p=snapshot.getJSONObject(i);
            if(LocalPoiCatalog.distance(point[0],point[1],p.getDouble("latitude"),p.getDouble("longitude"))<=LocalPoiCatalog.RADIUS_METERS)result.put(p);
        }
        if(result.length()==0)for(LocalPoiCatalog.Poi p:LocalPoiCatalog.within(point[0],point[1]))
            result.put(new JSONObject().put("title",p.name).put("description",p.description).put("source","施設公式情報")
                    .put("link",p.url).put("poi",true));
        Log.i("LokiTopicData","TOPIC/poi selectedCount="+result.length()+" radiusMeters=10000");return result;
    }
}
