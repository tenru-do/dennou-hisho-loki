package com.example.rokidgeminisecretary;

import android.content.Context;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class NavitimeNavigationClient {
    static final String HOST="navitime-route-totalnavi.p.rapidapi.com";
    static final String SIGNUP="https://rapidapi.com/navitimejapan-navitimejapan/api/navitime-route-totalnavi";
    static boolean enabled(Context c){return c.getSharedPreferences("loki_navitime_secure",0).contains("cipher");}
    static synchronized void reserve(Context c)throws Exception {
        android.content.SharedPreferences p=c.getSharedPreferences("loki_navitime_usage",0);
        long now=System.currentTimeMillis(),minute=now/60000;int used=p.getInt("requests",0);
        int recent=p.getLong("minute",0)==minute?p.getInt("recent",0):0;
        // Conservative local guard: never assume the remote free allowance reset.
        if(used>=450||recent>=10)throw new Exception("navitime_local_limit");
        if(!p.edit().putInt("requests",used+1).putLong("minute",minute).putInt("recent",recent+1).commit())throw new Exception("navitime_usage_save_failed");
    }
    private static JSONObject requestJson(Context c,String endpoint,String query)throws Exception {
        String key=NavitimeCredentialStore.read(c);if(key.isEmpty())throw new Exception("navitime_not_configured");
        reserve(c);HttpURLConnection conn=null;
        try {
            conn=(HttpURLConnection)new URL("https://"+HOST+"/"+endpoint+"?"+query).openConnection();
            conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(8000);conn.setReadTimeout(15000);conn.setUseCaches(false);
            conn.setRequestProperty("X-RapidAPI-Key",key);conn.setRequestProperty("X-RapidAPI-Host",HOST);
            int status=conn.getResponseCode();
            if(status!=200){
                // Only the status is persisted or logged; credentials and response bodies stay private.
                c.getSharedPreferences("loki_navitime_secure",0).edit().putString("last_result","navitime_http_"+status).apply();
                throw new Exception("navitime_http_"+status);
            }
            try(InputStream input=conn.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(bytes.size()+n>4000000)throw new Exception("navitime_response_too_large");bytes.write(buffer,0,n);}
                c.getSharedPreferences("loki_navitime_secure",0).edit().putString("last_result","http_200").putBoolean("verified",true).apply();
                return new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            }
        }catch(Exception error){String message=error.getMessage();if(message!=null&&message.matches("navitime_[a-z_0-9]+"))throw error;throw new Exception("navitime_request_failed");}
        finally{key="";if(conn!=null)conn.disconnect();}
    }
    static JSONObject request(Context c,double lat,double lng,double toLat,double toLng,String departure)throws Exception {
        return requestJson(c,"route_transit",NavitimeRequestPolicy.routeQuery(lat,lng,toLat,toLng,departure));
    }
    interface AccessListener { void ready(android.location.Address station,JSONObject route,String mode) throws Exception; }
    static JSONArray choices(Context c,double lat,double lng,String destination)throws Exception {
        double[] target=GoogleNavigationClient.destinationCoordinates(c,destination);
        String departure=c.getSharedPreferences("loki_navitime_secure",0).getString("departure","");
        long now=System.currentTimeMillis();
        if(!departure.isEmpty()){
            try{now=java.time.LocalDateTime.parse(departure).atZone(java.time.ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();}
            catch(Exception invalid){throw new Exception("navitime_invalid_departure");}
            if(now<System.currentTimeMillis()-60000)throw new Exception("navitime_departure_past");
        }
        String time=NavitimeRequestPolicy.departureTime(now);
        JSONObject raw=requestJson(c,"route_transit",NavitimeRequestPolicy.choicesQuery(lat,lng,target[0],target[1],time));
        JSONArray items=raw.optJSONArray("items"),out=new JSONArray();
        if(items==null||items.length()==0)throw new Exception("navitime_route_not_found");
        for(int i=0;i<Math.min(3,items.length());i++){
            JSONObject one=new JSONObject().put("items",new JSONArray().put(items.getJSONObject(i))),shape=null;
            if(NavitimeRouteAdapter.needsTransportGeometry(one)){
                JSONObject summary=items.getJSONObject(i).optJSONObject("summary");
                try{shape=requestJson(c,"shape_transit",NavitimeRequestPolicy.shapeQuery(lat,lng,target[0],target[1],time,summary==null?"":summary.optString("no")));}
                catch(Exception unavailable){ /* Adapter marks missing provider geometry as estimated. */ }
            }
            JSONObject route=NavitimeRouteAdapter.convert(one,shape,(a,b,x,y,m)->GoogleNavigationClient.routeToPoint(c,a,b,x,y,m));
            route.put("lokiTimetableBased",false);out.put(route);
        }
        return out;
    }
    private static String url(String value)throws Exception{return URLEncoder.encode(value,"UTF-8");}
    static JSONObject route(Context c,double lat,double lng,String destination)throws Exception {
        return route(c,lat,lng,destination,null);
    }
    static JSONObject route(Context c,double lat,double lng,String destination,AccessListener listener)throws Exception {
        if(!enabled(c))throw new Exception("navitime_not_configured");
        double[] target=GoogleNavigationClient.destinationCoordinates(c,destination);
        android.content.SharedPreferences p=c.getSharedPreferences("loki_navitime_secure",0);
        String departure=p.getString("departure","");
        long start=System.currentTimeMillis();
        if(!departure.isEmpty()) {
            try{start=java.time.LocalDateTime.parse(departure).atZone(java.time.ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();}
            catch(Exception invalid){throw new Exception("navitime_invalid_departure");}
            if(start<System.currentTimeMillis()-60000)throw new Exception("navitime_departure_past");
        }
        String time=NavitimeRequestPolicy.departureTime(start);
        JSONObject route;
        try {
            JSONObject raw=request(c,lat,lng,target[0],target[1],time);
            JSONObject shape=null;
            if(NavitimeRouteAdapter.needsTransportGeometry(raw)) {
                JSONArray items=raw.optJSONArray("items");
                JSONObject first=items==null?null:items.optJSONObject(0);
                JSONObject summary=first==null?null:first.optJSONObject("summary");
                String number=summary==null?"":summary.optString("no","");
                try{shape=requestJson(c,"shape_transit",NavitimeRequestPolicy.shapeQuery(lat,lng,target[0],target[1],time,number));}catch(Exception unavailable){android.util.Log.i("LokiTransit","shape unavailable; using segment nodes");}
            }
            route=NavitimeRouteAdapter.convert(raw,shape,(a,b,x,y,mode)->GoogleNavigationClient.routeToPoint(c,a,b,x,y,mode));
        } catch(Exception failure) {
            throw failure;
        }
        route.put("lokiTimetableBased",false); // API-market default uses average rail travel times.
        return route;
    }
}
