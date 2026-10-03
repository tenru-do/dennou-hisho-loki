package com.example.rokidgeminisecretary;
import java.util.ArrayList;
final class GooglePolyline {
    static void mergeStepsOrOverview(org.json.JSONObject route) throws org.json.JSONException {
        org.json.JSONObject overview = route.optJSONObject("polyline");
        try { mergeSteps(route); }
        catch (Exception invalidSteps) {
            if (overview == null) throw new org.json.JSONException("Missing route geometry");
            decode(overview.optString("encodedPolyline"));
            route.put("polyline", overview);
        }
    }
    static double[] location(org.json.JSONObject value) {
        if(value==null)return null;
        org.json.JSONObject p=value.optJSONObject("latLng");if(p==null)p=value;
        double lat=p.optDouble("latitude",Double.NaN),lng=p.optDouble("longitude",Double.NaN);
        if(!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180)return null;
        return new double[]{lat,lng};
    }
    static String encode(java.util.List<double[]> points) {
        StringBuilder encoded=new StringBuilder();long lat=0,lng=0;
        for(double[] p:points){long a=Math.round(p[0]*1e5),b=Math.round(p[1]*1e5);
            encodeComponent(encoded,a-lat);encodeComponent(encoded,b-lng);lat=a;lng=b;}
        return encoded.toString();
    }
    private static double[] endpoint(org.json.JSONObject step,boolean start) {
        double[] p=location(step.optJSONObject(start?"startLocation":"endLocation"));
        if(p!=null)return p;
        org.json.JSONObject details=step.optJSONObject("transitDetails");
        org.json.JSONObject stops=details==null?null:details.optJSONObject("stopDetails");
        org.json.JSONObject stop=stops==null?null:stops.optJSONObject(start?"departureStop":"arrivalStop");
        return location(stop==null?null:stop.optJSONObject("location"));
    }
    static void mergeSteps(org.json.JSONObject route) throws org.json.JSONException {
        ArrayList<double[]> all=new ArrayList<>();
        org.json.JSONArray segments=new org.json.JSONArray(),legs=route.optJSONArray("legs");
        if(legs==null)return;
        for(int i=0;i<legs.length();i++){
            org.json.JSONObject leg=legs.optJSONObject(i);if(leg==null)continue;
            org.json.JSONArray steps=leg.optJSONArray("steps");if(steps==null)continue;
            for(int j=0;j<steps.length();j++){
                org.json.JSONObject step=steps.optJSONObject(j);if(step==null)continue;
                org.json.JSONObject line=step.optJSONObject("polyline");double[][] points=null;boolean estimated=step.optBoolean("lokiGeometryEstimated",false);
                if(line!=null)try{points=decode(line.optString("encodedPolyline"));}catch(Exception ignored){}
                if(points==null){
                    double[] a=endpoint(step,true),b=endpoint(step,false);
                    if(a==null && j==0)a=location(leg.optJSONObject("startLocation"));
                    if(b==null && j==steps.length()-1)b=location(leg.optJSONObject("endLocation"));
                    if(a==null && !all.isEmpty())a=all.get(all.size()-1);
                    if(b==null && j+1<steps.length()) {org.json.JSONObject next=steps.optJSONObject(j+1);if(next!=null)b=endpoint(next,true);}
                    if(a==null||b==null)continue;
                    points=new double[][]{a,b};estimated=true;
                    step.put("polyline",new org.json.JSONObject().put("encodedPolyline",encode(java.util.Arrays.asList(points))));
                    step.put("lokiGeometryEstimated",true);
                }
                org.json.JSONArray path=new org.json.JSONArray();
                for(double[] p:points){path.put(new org.json.JSONArray().put(p[0]).put(p[1]));
                    double[] last=all.isEmpty()?null:all.get(all.size()-1);
                    if(last==null||Math.round(last[0]*1e5)!=Math.round(p[0]*1e5)||Math.round(last[1]*1e5)!=Math.round(p[1]*1e5))all.add(p);}
                segments.put(new org.json.JSONObject().put("travelMode",step.optString("travelMode")).put("estimated",estimated).put("points",path));
            }
        }
        route.put("lokiRouteSegments",segments);
        if(all.size()>=2)route.put("polyline",new org.json.JSONObject().put("encodedPolyline",encode(all)));
    }
    private static void encodeComponent(StringBuilder out, long delta) {
        long value = delta < 0 ? ~(delta << 1) : delta << 1;
        while (value >= 32) { out.append((char)((value & 31) + 95)); value >>= 5; }
        out.append((char)(value + 63));
    }
    static double[][] decode(String encoded) {
        ArrayList<double[]> points = new ArrayList<double[]>();
        int[] cursor = {0};
        long lat = 0, lng = 0;
        while (cursor[0] < encoded.length()) {
            lat += component(encoded, cursor);
            lng += component(encoded, cursor);
            if (Math.abs(lat) > 9000000 || Math.abs(lng) > 18000000 || points.size() >= 100000)
                throw new IllegalArgumentException("invalid polyline");
            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        if (points.size() < 2) throw new IllegalArgumentException("empty polyline");
        return points.toArray(new double[points.size()][]);
    }
    private static long component(String s, int[] cursor) {
        long value = 0;
        for (int shift = 0; shift <= 30; shift += 5) {
            if (cursor[0] >= s.length()) throw new IllegalArgumentException("truncated polyline");
            int b = s.charAt(cursor[0]++) - 63;
            if (b < 0 || b > 63) throw new IllegalArgumentException("invalid polyline");
            value |= (long)(b & 31) << shift;
            if (b < 32) return (value & 1) == 0 ? value >> 1 : ~(value >> 1);
        }
        throw new IllegalArgumentException("invalid polyline");
    }
}
