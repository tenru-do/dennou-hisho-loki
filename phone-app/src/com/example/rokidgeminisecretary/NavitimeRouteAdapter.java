package com.example.rokidgeminisecretary;

import org.json.*;
import java.util.*;

/** NAVITIME points/moves to the existing immutable journey model. No invented rail geometry. */
final class NavitimeRouteAdapter {
    interface RoadRouter { JSONObject route(double lat, double lng, double toLat, double toLng, String mode) throws Exception; }
    static String encode(List<double[]> points) {
        StringBuilder out=new StringBuilder();long lat=0,lng=0;
        for(double[] p:points){long a=Math.round(p[0]*1e5),b=Math.round(p[1]*1e5);component(out,a-lat);component(out,b-lng);lat=a;lng=b;}
        return out.toString();
    }
    private static void component(StringBuilder out,long delta){long v=delta<0?~(delta<<1):delta<<1;while(v>=32){out.append((char)((v&31)+95));v>>=5;}out.append((char)(v+63));}
    private static double[] coordinate(double a,double b)throws Exception {
        if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180)throw new Exception("navitime_invalid_coordinate");
        return new double[]{a,b};
    }
    private static double[] point(JSONObject node)throws Exception {
        JSONObject c=node.getJSONObject("coord");return coordinate(c.getDouble("lat"),c.getDouble("lon"));
    }
    private static JSONObject firstItem(JSONObject response)throws Exception {
        JSONArray items=response.optJSONArray("items");
        if(items==null||items.length()==0)throw new Exception("navitime_route_not_found");
        return items.getJSONObject(0);
    }
    private static boolean roadMode(String mode){return "walk".equals(mode)||"car".equals(mode)||"bicycle".equals(mode);}
    /** /route_transit can return walking-only shapes; /shape_transit adds transport_shape. */
    static boolean needsTransportGeometry(JSONObject response)throws Exception {
        JSONObject item=firstItem(response);JSONArray sections=item.getJSONArray("sections");boolean transit=false;
        for(int i=0;i<sections.length();i++){JSONObject s=sections.getJSONObject(i);if("move".equals(s.optString("type"))&&!roadMode(s.optString("move")))transit=true;}
        if(!transit)return false;
        JSONObject shapes=item.optJSONObject("shapes");JSONArray features=shapes==null?null:shapes.optJSONArray("features");
        if(features==null)return true;
        for(int i=0;i<features.length();i++){
            JSONObject feature=features.getJSONObject(i),properties=feature.optJSONObject("properties");
            JSONObject geometry=feature.optJSONObject("geometry");
            if(geometry!=null&&"LineString".equals(geometry.optString("type"))
                    &&(properties==null||"transport".equals(properties.optString("ways"))))return false;
        }
        return true;
    }
    private static final class Shapes {
        final List<double[]> points=new ArrayList<>();
        final Set<Integer> breaks=new HashSet<>();
        int cursor;
    }
    private static Shapes transportShapes(JSONObject item,JSONObject separate)throws Exception {
        JSONObject collection=separate!=null?separate:item.optJSONObject("shapes");
        // /shape_transit format=geojson returns the FeatureCollection at the root.
        JSONArray features=collection==null?null:collection.optJSONArray("features");Shapes result=new Shapes();
        if(features==null)return result;
        boolean separated=true;
        JSONObject summary=item.optJSONObject("summary");String expected=summary==null?"":summary.optString("no");
        for(int i=0;i<features.length();i++){
            JSONObject feature=features.getJSONObject(i),properties=feature.optJSONObject("properties");
            String actual=properties==null?"":properties.optString("route_no");
            if(!expected.isEmpty()&&!actual.isEmpty()&&!expected.equals(actual))throw new Exception("navitime_shape_route_mismatch");
            if(properties!=null&&!"transport".equals(properties.optString("ways"))){separated=true;continue;}
            JSONObject g=feature.optJSONObject("geometry");
            if(g==null||!"LineString".equals(g.optString("type")))continue;
            JSONArray coords=g.getJSONArray("coordinates");
            for(int j=0;j<coords.length();j++){
                JSONArray p=coords.getJSONArray(j);double[] next=coordinate(p.getDouble(1),p.getDouble(0));
                if(j==0&&!result.points.isEmpty()&&(separated||metres(result.points.get(result.points.size()-1),next)>40))result.breaks.add(result.points.size());
                result.points.add(next);
            }
            separated=false;
        }
        return result;
    }
    private static double metres(double[] a,double[] b){double x=(a[0]-b[0])*111320,y=(a[1]-b[1])*111320*Math.cos(Math.toRadians(a[0]));return Math.sqrt(x*x+y*y);}
    private static int nearest(List<double[]> points,double[] p,int begin,int limit){int best=-1;double d=Double.MAX_VALUE;for(int i=begin;i<limit;i++){double v=metres(points.get(i),p);if(v<d){d=v;best=i;}}return best;}
    private static List<double[]> segment(Shapes shapes,double[] from,double[] to)throws Exception {
        int bestStart=-1,bestEnd=-1,begin=0;double score=Double.MAX_VALUE;
        // Match within a continuous provider shape, not across walking gaps. A forward
        // cursor distinguishes the second occurrence of a station on a return journey.
        for(int limit=1;limit<=shapes.points.size();limit++){
            if(limit!=shapes.points.size()&&!shapes.breaks.contains(limit))continue;
            int start=nearest(shapes.points,from,Math.max(begin,shapes.cursor),limit);
            int end=start<0?-1:nearest(shapes.points,to,start+1,limit);
            if(start>=0&&end>=0){double a=metres(shapes.points.get(start),from),b=metres(shapes.points.get(end),to);
                if(a<=400&&b<=400&&a+b<score){bestStart=start;bestEnd=end;score=a+b;}}
            begin=limit;
        }
        if(bestStart<0)return Arrays.asList(from,to);
        shapes.cursor=bestEnd;return shapes.points.subList(bestStart,bestEnd+1);
    }
    private static double seconds(JSONObject move) {
        double minutes=move.optDouble("time",Double.NaN);
        if(Double.isFinite(minutes)&&minutes>=0)return minutes*60;
        try{return Math.max(0,java.time.Duration.between(java.time.OffsetDateTime.parse(move.getString("from_time")),java.time.OffsetDateTime.parse(move.getString("to_time"))).getSeconds());}
        catch(Exception absent){return 0;}
    }
    private static String lineName(JSONObject move,JSONObject transport){
        String name=transport==null?"":transport.optString("self_name").trim();
        if(name.isEmpty())name=move.optString("line_name").trim();
        if(name.isEmpty()&&transport!=null)name=transport.optString("name").trim();
        return name.isEmpty()?"公共交通":name;
    }
    private static String vehicle(String mode)throws Exception {
        if(mode.endsWith("bus"))return "BUS";
        if("domestic_flight".equals(mode))return "AIRPLANE";
        if("ferry".equals(mode))return "FERRY";
        if(mode.endsWith("train")||"sleeper_ultraexpress".equals(mode))return "TRAIN";
        throw new Exception("navitime_unknown_mode");
    }
    private static boolean zeroRoadStep(JSONObject step){
        if(step.optDouble("distanceMeters",0)!=0||TransitJourney.duration(step.optString("staticDuration","0s"))!=0)return false;
        JSONObject a=step.optJSONObject("startLocation"),b=step.optJSONObject("endLocation");
        a=a==null?null:a.optJSONObject("latLng");b=b==null?null:b.optJSONObject("latLng");
        return a!=null&&b!=null&&Double.isFinite(a.optDouble("latitude",Double.NaN))
            &&Double.isFinite(a.optDouble("longitude",Double.NaN))
            &&a.optDouble("latitude") == b.optDouble("latitude",Double.NaN)
            &&a.optDouble("longitude") == b.optDouble("longitude",Double.NaN);
    }
    private static List<double[]> roadGeometry(JSONObject road)throws Exception {
        JSONObject overview=road.optJSONObject("polyline");
        if(overview!=null)try{return Arrays.asList(GooglePolyline.decode(overview.optString("encodedPolyline")));}catch(IllegalArgumentException absent){}
        List<double[]> points=new ArrayList<>();JSONArray legs=road.getJSONArray("legs");
        for(int i=0;i<legs.length();i++){
            JSONArray steps=legs.getJSONObject(i).getJSONArray("steps");
            for(int j=0;j<steps.length();j++){
                JSONObject step=steps.getJSONObject(j);if(zeroRoadStep(step))continue;
                JSONObject shape=step.optJSONObject("polyline");
                if(shape==null)throw new Exception("navitime_road_geometry_unavailable");
                try{points.addAll(Arrays.asList(GooglePolyline.decode(shape.optString("encodedPolyline"))));}
                catch(IllegalArgumentException missing){throw new Exception("navitime_road_geometry_unavailable");}
            }
        }
        if(points.size()<2)throw new Exception("navitime_road_geometry_unavailable");return points;
    }
    static JSONObject convert(JSONObject response,RoadRouter roads)throws Exception {return convert(response,null,roads);}
    static JSONObject convert(JSONObject response,JSONObject shapeResponse,RoadRouter roads)throws Exception {
        JSONObject item=firstItem(response);Shapes shapes;try{shapes=transportShapes(item,shapeResponse);}catch(Exception invalid){shapes=new Shapes();}
        JSONArray sections=item.getJSONArray("sections"),steps=new JSONArray();List<double[]> wholeGeometry=new ArrayList<>();
        for(int i=0;i<sections.length();i++){
            JSONObject move=sections.getJSONObject(i);if(!"move".equals(move.optString("type")))continue;
            if(i==0||i+1>=sections.length())throw new Exception("navitime_invalid_sections");
            JSONObject from=sections.getJSONObject(i-1),to=sections.getJSONObject(i+1);
            if(!"point".equals(from.optString("type"))||!"point".equals(to.optString("type")))throw new Exception("navitime_invalid_sections");
            double[] a=point(from),b=point(to);String mode=move.getString("move");
            if(roadMode(mode)){
                String requested="car".equals(mode)?"driving":"bicycle".equals(mode)?"bicycling":"walking";
                JSONObject road;try{road=roads.route(a[0],a[1],b[0],b[1],requested);roadGeometry(road);}catch(Exception unavailable){
                    JSONObject fallback=new JSONObject().put("travelMode","WALK").put("startLocation",location(a)).put("endLocation",location(b)).put("staticDuration",seconds(move)+"s").put("distanceMeters",move.optDouble("distance",0)).put("lokiGeometryEstimated",true).put("polyline",new JSONObject().put("encodedPolyline",encode(Arrays.asList(a,b))));
                    road=new JSONObject().put("legs",new JSONArray().put(new JSONObject().put("steps",new JSONArray().put(fallback)))).put("polyline",fallback.getJSONObject("polyline"));
                }JSONArray legs=road.getJSONArray("legs");
                wholeGeometry.addAll(roadGeometry(road));
                int before=steps.length();
                for(int l=0;l<legs.length();l++){JSONArray roadSteps=legs.getJSONObject(l).getJSONArray("steps");for(int k=0;k<roadSteps.length();k++){
                    JSONObject step=roadSteps.getJSONObject(k);if(!zeroRoadStep(step))steps.put(step);
                }}
                if(steps.length()==before)throw new Exception("navitime_road_geometry_unavailable");
            }else{
                String type=vehicle(mode);List<double[]> path=segment(shapes,a,b);JSONObject transport=move.optJSONObject("transport");
                wholeGeometry.addAll(path);
                JSONObject stops=new JSONObject().put("departureStop",new JSONObject().put("name",from.optString("name")))
                    .put("arrivalStop",new JSONObject().put("name",to.optString("name")))
                    .put("departureTime",move.optString("from_time")).put("arrivalTime",move.optString("to_time"));
                JSONObject line=new JSONObject().put("name",lineName(move,transport)).put("vehicle",new JSONObject().put("type",type));
                JSONObject step=new JSONObject().put("travelMode","TRANSIT").put("staticDuration",seconds(move)+"s")
                    .put("distanceMeters",move.optDouble("distance",0)).put("startLocation",location(a)).put("endLocation",location(b))
                    .put("polyline",new JSONObject().put("encodedPolyline",encode(path))).put("lokiGeometryEstimated",path.size()==2)
                    .put("navigationInstruction",new JSONObject().put("instructions",line.getString("name")))
                    .put("transitDetails",new JSONObject().put("stopDetails",stops).put("transitLine",line));
                if(transport!=null&&transport.optJSONObject("fare")!=null)step.put("lokiNavitimeFare",transport.getJSONObject("fare"));
                steps.put(step);
            }
        }
        if(steps.length()==0)throw new Exception("navitime_invalid_sections");
        JSONObject summary=item.optJSONObject("summary"),move=summary==null?null:summary.optJSONObject("move");
        double total=0;for(int i=0;i<steps.length();i++)total+=Math.max(0,TransitJourney.duration(steps.getJSONObject(i).optString("staticDuration")));
        JSONObject route=new JSONObject().put("legs",new JSONArray().put(new JSONObject().put("steps",steps)))
            .put("duration",(move==null?total:seconds(move))+"s").put("distanceMeters",move==null?0:move.optDouble("distance",0))
            // Some Google indoor steps contain a single point despite positive distance.
            // Retain their instructions and metrics, using that interval's API overview
            // if a complete step-by-step polyline cannot be merged.
            .put("polyline",new JSONObject().put("encodedPolyline",encode(wholeGeometry))).put("lokiProvider","navitime");
        if(move!=null&&move.optJSONObject("fare")!=null)route.put("lokiNavitimeFare",move.getJSONObject("fare"));
        GooglePolyline.mergeSteps(route);
        try{GooglePolyline.decode(route.getJSONObject("polyline").getString("encodedPolyline"));}
        catch(Exception missing){throw new Exception("navitime_geometry_unavailable");}
        return route;
    }
    private static JSONObject location(double[] p)throws Exception{return new JSONObject().put("latLng",new JSONObject().put("latitude",p[0]).put("longitude",p[1]));}
}
