package com.example.rokidgeminisecretary;
import org.json.*;
/** Preserve a road route's valid overview when individual indoor steps lack shape. */
final class WalkingAccessGeometry {
    interface Router {JSONObject route(double a,double b,double c,double d,String mode)throws Exception;}
    static boolean valid(JSONObject step) {
        try{GooglePolyline.decode(step.getJSONObject("polyline").getString("encodedPolyline"));return true;}
        catch(Exception missing){return false;}
    }
    static JSONArray roadSteps(JSONObject road,String mode)throws Exception {
        GooglePolyline.normalize(road);
        JSONArray out=new JSONArray(),legs=road.getJSONArray("legs");boolean complete=true;
        for(int i=0;i<legs.length();i++){JSONArray steps=legs.getJSONObject(i).getJSONArray("steps");for(int j=0;j<steps.length();j++){
            JSONObject step=steps.getJSONObject(j);out.put(step);if(!valid(step))complete=false;
        }}
        if(complete && out.length()>0)return out;
        JSONObject overview=road.optJSONObject("polyline");
        if(overview==null || out.length()==0)return out;
        double[][] points=GooglePolyline.decode(overview.getString("encodedPolyline"));
        StringBuilder instructions=new StringBuilder();
        for(int i=0;i<out.length();i++){JSONObject nav=out.getJSONObject(i).optJSONObject("navigationInstruction");if(nav!=null){if(instructions.length()>0)instructions.append(" / ");instructions.append(nav.optString("instructions"));}}
        JSONObject first=out.getJSONObject(0),last=out.getJSONObject(out.length()-1);
        JSONObject merged=new JSONObject(first.toString()).put("travelMode",mode).put("polyline",overview)
            .put("endLocation",last.optJSONObject("endLocation")==null?location(points[points.length-1]):last.getJSONObject("endLocation"))
            .put("startLocation",first.optJSONObject("startLocation")==null?location(points[0]):first.getJSONObject("startLocation"))
            .put("distanceMeters",road.optDouble("distanceMeters",-1)).put("staticDuration",road.optString("duration",""))
            .put("navigationInstruction",new JSONObject().put("instructions",instructions.toString())).put("lokiGeometryEstimated",false);
        return new JSONArray().put(merged);
    }
    private static JSONObject location(double[] p)throws Exception{return new JSONObject().put("latLng",new JSONObject().put("latitude",p[0]).put("longitude",p[1]));}
    static void repairLeading(JSONObject route,Router router)throws Exception {
        GooglePolyline.normalize(route);JSONArray legs=route.optJSONArray("legs");if(legs==null||legs.length()==0)return;
        JSONObject leg=legs.getJSONObject(0);JSONArray steps=leg.optJSONArray("steps");if(steps==null)return;
        int count=0;boolean missing=false;
        while(count<steps.length() && "WALK".equals(steps.getJSONObject(count).optString("travelMode"))){missing|=!valid(steps.getJSONObject(count));count++;}
        if(count==0||!missing)return;
        double[] a=GooglePolyline.location(steps.getJSONObject(0).optJSONObject("startLocation"));
        double[] b=GooglePolyline.location(steps.getJSONObject(count-1).optJSONObject("endLocation"));
        if(a==null||b==null)return;
        JSONArray road=roadSteps(router.route(a[0],a[1],b[0],b[1],"walking"),"WALK");
        for(int i=0;i<road.length();i++)if(!valid(road.getJSONObject(i)))throw new Exception("walking_access_geometry_unavailable");
        if(road.length()==0)throw new Exception("walking_access_geometry_unavailable");
        JSONArray combined=new JSONArray();for(int i=0;i<road.length();i++)combined.put(road.getJSONObject(i));
        for(int i=count;i<steps.length();i++)combined.put(steps.getJSONObject(i));leg.put("steps",combined);
    }
}
