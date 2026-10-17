package com.example.rokidgeminisecretary;
import org.json.*;
import java.util.*;
public final class MultimodalJourneyTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static JSONObject location(double lat,double lng)throws Exception{return new JSONObject().put("latLng",new JSONObject().put("latitude",lat).put("longitude",lng));}
    static JSONObject ride(String from,String to,double lat,double lng,double x,double y)throws Exception {
        return new JSONObject().put("travelMode","TRANSIT").put("startLocation",location(lat,lng)).put("endLocation",location(x,y))
            .put("transitDetails",new JSONObject().put("stopDetails",new JSONObject()
                .put("departureStop",new JSONObject().put("name",from)).put("arrivalStop",new JSONObject().put("name",to))));
    }
    static JSONObject route(JSONObject... steps)throws Exception{return new JSONObject().put("legs",new JSONArray().put(new JSONObject().put("steps",new JSONArray(Arrays.asList(steps)))));}
    public static void main(String[] args)throws Exception {
        MultimodalJourney p=new MultimodalJourney("final",new double[]{35.1,139.1},"bicycling","wrong");
        check(p.mode().equals("bicycling"),"explicit cycle retained"); check(p.egressMode.equals("walking"),"default walk");
        check(new MultimodalJourney("final",new double[]{35.1,139.1},"","wrong").mode().equals("walking"),"unconfigured access defaults to walking");
        check(!p.advance("board"),"cannot board before route");
        p.stations(route(ride("A","B",35,139,35.04,139.04),new JSONObject().put("travelMode","WALK"),ride("B","C",35.04,139.04,35.08,139.08)));
        check(p.boardingName.equals("A")&&p.alightingName.equals("C"),"outer stations across transfer");
        check(Arrays.equals(p.target(),new double[]{35,139}),"access target");
        List<String> calls=new ArrayList<>();
        MultimodalJourney.RouteRequest api=(a,b,x,y,m)-> {
            calls.add(m+":"+a+":"+x);
            check(m.equals("bicycling")?x==35:m.equals("transit")?a==35&&x==35.08:a==35.08&&x==35.1,"independent phase endpoints");
            double[][] points=m.equals("bicycling")?new double[][]{{34.99,139},{34.99,139.005},{35,139}}:
                m.equals("transit")?new double[][]{{35,139},{35.04,139.04},{35.08,139.08}}:
                new double[][]{{35.08,139.08},{35.09,139.08},{35.1,139.1}};
            return new JSONObject().put("polyline",new JSONObject().put("encodedPolyline",GooglePolyline.encode(Arrays.asList(points))));
        };
        JSONObject first=p.fetchPhase(34.99,139,api); p.ready=true;
        long now=System.currentTimeMillis();
        check(p.geofence(35,139,10,now,now).equals("arrive"),"near stop means arrival, not automatic boarding");
        check(p.geofence(35.001,139,10,now,now).isEmpty(),"outside 70m");
        check(p.geofence(35,139,71,now,now).isEmpty(),"inaccurate GPS");
        check(p.geofence(35,139,10,now-31000,now).isEmpty(),"stale GPS");
        check(p.geofence(Double.NaN,139,10,now,now).isEmpty(),"GPS absent");
        check(!p.advance("alight"),"out of order action");
        check(p.advance("board"),"manual board without GPS");
        check(!p.advance("board")&&!p.advance("alight"),"duplicate and fetching guard");
        check(p.mode().equals("transit"),"transit mode");
        JSONObject train=p.fetchPhase(Double.NaN,Double.NaN,api); p.ready=true;
        check(!first.toString().equals(train.toString()),"polyline switch to transit");
        check(p.geofence(35.08,139.08,10,now,now).isEmpty(),"pre-transition fix rejected");
        now=System.currentTimeMillis()+100;
        check(p.geofence(35.08,139.08,10,now,now).equals("alight"),"automatic alight");
        check(p.geofence(35.04,139.04,10,now,now).isEmpty(),"transfer station not final alight");
        check(p.advance("alight"),"manual alight without GPS");
        check(p.mode().equals("walking"),"last mile walking");
        JSONObject last=p.fetchPhase(Double.NaN,Double.NaN,api); p.ready=true;
        check(!last.toString().equals(train.toString()),"polyline switch to walking");
        check(!p.advance("alight")&&!p.advance("board"),"terminal phase");
        check(p.geofence(35.1,139.1,10,now,now).isEmpty(),"no phase four");
        check(calls.size()==2,"road phases requested; selected transit preserved without another provider query");
        check(GooglePolyline.decode(first.getJSONObject("polyline").getString("encodedPolyline")).length==3,"retained cycle road vertex");
        check(GooglePolyline.decode(last.getJSONObject("polyline").getString("encodedPolyline"))[1][0]==35.09,"retained walk road vertex");
        MultimodalJourney reverse=new MultimodalJourney("final",new double[]{35.1,139.1},"walking","bicycling");
        check(reverse.mode().equals("walking")&&reverse.egressMode.equals("bicycling"),"reversed selections");
        boolean failed=false;try{p.fetchPhase(0,0,(a,b,x,y,m)->{throw new Exception("google_route_not_found");});}catch(Exception e){failed=true;}
        check(failed,"API error remains visible, no implicit fallback");
        failed=false;try{reverse.stations(route(new JSONObject().put("travelMode","WALK")));}catch(Exception e){failed=true;}
        check(failed,"no transit stations rejected");
        MultimodalJourney overview=new MultimodalJourney("final",new double[]{35.1,139.1},"bicycling","walking");
        JSONObject access=new JSONObject().put("travelMode","WALK").put("staticDuration","100s").put("distanceMeters",100);
        JSONObject rail=ride("A","C",35,139,35.08,139.08).put("staticDuration","500s").put("distanceMeters",8000)
            .put("transitDetails",new JSONObject().put("transitLine",new JSONObject().put("name","train"))
                .put("stopDetails",new JSONObject().put("departureStop",new JSONObject().put("name","A"))
                .put("arrivalStop",new JSONObject().put("name","C"))));
        JSONObject egress=new JSONObject().put("travelMode","WALK").put("staticDuration","200s").put("distanceMeters",200);
        overview.stations(route(access,rail,egress).put("duration","800s").put("distanceMeters",8300));
        overview.egressPreview=route(egress).put("duration","180s").put("distanceMeters",300);
        JSONObject cycle=route(new JSONObject().put("travelMode","BICYCLE").put("staticDuration","60s").put("distanceMeters",400))
            .put("duration","60s").put("distanceMeters",400).put("polyline",first.getJSONObject("polyline"));
        JSONObject summary=overview.overview(cycle,100000);
        check(summary.getDouble("durationSeconds")==740,"whole overview replaces original first and last miles");
        check(summary.getDouble("distanceMeters")==8700,"whole overview distance");
        check(summary.getJSONArray("transitStops").length()==1,"first mile retains future line");
        check(summary.getBoolean("transitRefreshPending"),"initial timetable flagged provisional");
        overview.ready=true;overview.advance("board");
        JSONObject trainRoute=route(rail).put("duration","550s").put("distanceMeters",8000).put("polyline",train.getJSONObject("polyline"));
        summary=overview.overview(trainRoute,100000);
        check(summary.getDouble("durationSeconds")==730,"refreshed transit overview includes egress");
        check(!summary.getBoolean("transitRefreshPending"),"transit refreshed");
        overview.ready=true;overview.advance("alight");
        JSONObject walk=route(egress).put("duration","190s").put("distanceMeters",320).put("polyline",last.getJSONObject("polyline"));
        summary=overview.overview(walk,100000);
        check(summary.getDouble("durationSeconds")==190&&summary.getDouble("distanceMeters")==320,"last mile actual replaces preview");
        check(summary.getJSONArray("transitStops").length()==0,"no departed trains in remaining journey");
        System.out.println("MultimodalJourney PASS checks="+checks);
    }
}
