package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/** Optional OSM map annotation, never a crossing-safety decision. Rounded area, not exact GPS, is queried. */
final class SignalLocations {
    private static JSONArray data = new JSONArray();
    private static double centerLat=Double.NaN, centerLng=Double.NaN;
    private static long fetchedAt, retryAt;
    private static boolean busy;
    private static String state="unrequested";
    static synchronized JSONObject snapshot(final double lat, final double lng) throws Exception {
        long now=System.currentTimeMillis();
        boolean near = Double.isFinite(centerLat) && Math.abs(lat-centerLat)<0.004 && Math.abs(lng-centerLng)<0.006;
        if (!busy && now>=retryAt && (!near || now-fetchedAt>3600000L)) {
            busy=true; state="loading"; retryAt=now+300000L;
            final double roundedLat=Math.rint(lat*500)/500, roundedLng=Math.rint(lng*500)/500;
            new Thread(new Runnable() { public void run() { fetch(roundedLat,roundedLng); } },"SignalLocations").start();
        }
        return new JSONObject().put("state",near ? state : (busy ? "loading" : "unavailable"))
                .put("points",near && now-fetchedAt<86400000L ? data : new JSONArray())
                .put("source","信号: © OpenStreetMap contributors");
    }
    private static void fetch(double lat,double lng) {
        HttpURLConnection c=null;
        try {
            String box=String.format(Locale.US,"%.4f,%.4f,%.4f,%.4f",lat-0.008,lng-0.012,lat+0.008,lng+0.012);
            String query="[out:json][timeout:12];(node[highway=traffic_signals]("+box+");node[crossing=traffic_signals]("+box+");node[\"crossing:signals\"=yes]("+box+" ););out body 200;";
            c=(HttpURLConnection)new URL("https://overpass-api.de/api/interpreter").openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true); c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(6000); c.setReadTimeout(15000);
            c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
            c.setRequestProperty("User-Agent","DennoHishoLoki/0.9 (https://github.com/tenru-do/dennou-hisho-loki)");
            byte[] body=("data="+URLEncoder.encode(query,"UTF-8")).getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(body.length);
            try(java.io.OutputStream out=c.getOutputStream()){out.write(body);}
            if(c.getResponseCode()!=200)throw new Exception("signal_service_unavailable");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=c.getInputStream()){
                byte[] b=new byte[4096];int n;
                while((n=input.read(b))!=-1){if(out.size()+n>1048576)throw new Exception("signal_response_large");out.write(b,0,n);}
            }
            JSONObject root=new JSONObject(out.toString("UTF-8"));
            if(root.has("remark"))throw new Exception("signal_incomplete");
            JSONArray elements=root.getJSONArray("elements"), points=new JSONArray();
            for(int i=0;i<elements.length();i++){
                JSONObject e=elements.getJSONObject(i);
                double a=e.getDouble("lat"),o=e.getDouble("lon");
                if(!Double.isFinite(a)||!Double.isFinite(o)||Math.abs(a-lat)>0.009||Math.abs(o-lng)>0.013)continue;
                boolean duplicate=false;
                for(int j=0;j<points.length();j++){
                    JSONArray p=points.getJSONArray(j);
                    if(Math.abs(p.getDouble(0)-a)<0.0001 && Math.abs(p.getDouble(1)-o)<0.00012){duplicate=true;break;}
                }
                if(!duplicate)points.put(new JSONArray().put(a).put(o));
            }
            synchronized(SignalLocations.class){data=points;centerLat=lat;centerLng=lng;fetchedAt=System.currentTimeMillis();state="available";}
        }catch(Exception ignored){synchronized(SignalLocations.class){state="unavailable";}}
        finally{if(c!=null)c.disconnect();synchronized(SignalLocations.class){busy=false;}}
    }
}
