package com.example.rokidgeminisecretary;

import org.json.*;

/** Session-local plan. Route publication and phase advancement are separate transactions. */
final class MultimodalJourney {
    enum Phase { FIRST_MILE, TRANSIT_LEG, LAST_MILE }
    final String destination, accessMode, egressMode;
    final double[] finalPoint;
    volatile Phase phase = Phase.FIRST_MILE;
    double[] boarding, alighting;
    String boardingName = "", alightingName = "";
    JSONObject initialTransit, egressPreview;
    volatile boolean ready;
    long advancedAt;
    long revision;
    MultimodalJourney(String destination, double[] finalPoint, String access, String egress) {
        this.destination = destination; this.finalPoint = finalPoint.clone();
        accessMode = "walking".equals(access) ? "walking" : "bicycling";
        egressMode = "bicycling".equals(egress) ? "bicycling" : "walking";
    }
    synchronized void stations(JSONObject route) throws Exception {
        double[] first = null, last = null; String firstName = "", lastName = "";
        JSONArray legs = route.getJSONArray("legs");
        for (int i=0;i<legs.length();i++) {
            JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
            for (int j=0;j<steps.length();j++) {
                JSONObject step = steps.getJSONObject(j);
                if (!"TRANSIT".equals(step.optString("travelMode"))) continue;
                JSONObject stops = step.getJSONObject("transitDetails").getJSONObject("stopDetails");
                if (first == null) { first = stopPoint(stops.optJSONObject("departureStop"), step.getJSONObject("startLocation"));
                    firstName = stops.getJSONObject("departureStop").optString("name"); }
                last = stopPoint(stops.optJSONObject("arrivalStop"), step.getJSONObject("endLocation"));
                lastName = stops.getJSONObject("arrivalStop").optString("name");
            }
        }
        if (first == null || last == null) throw new Exception("google_transit_stations_missing");
        if (phase == Phase.FIRST_MILE) { boarding=first; boardingName=firstName; }
        alighting=last; alightingName=lastName;
        if (phase == Phase.FIRST_MILE) initialTransit = new JSONObject(route.toString());
    }
    private static double[] stopPoint(JSONObject stop, JSONObject fallback) throws Exception {
        JSONObject location = stop == null ? null : stop.optJSONObject("location");
        JSONObject point = (location == null ? fallback : location).getJSONObject("latLng");
        double lat=point.getDouble("latitude"), lng=point.getDouble("longitude");
        if (!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180)
            throw new Exception("google_transit_stations_missing");
        return new double[]{lat,lng};
    }
    synchronized String mode() { return phase==Phase.FIRST_MILE ? accessMode : phase==Phase.LAST_MILE ? egressMode : "transit"; }
    interface RouteRequest { JSONObject route(double lat,double lng,double toLat,double toLng,String mode) throws Exception; }
    JSONObject fetchPhase(double lat,double lng,RouteRequest request) throws Exception {
        double[] start, end; String travel;
        synchronized(this) {
            start=phase==Phase.FIRST_MILE ? new double[]{lat,lng} : (phase==Phase.TRANSIT_LEG ? boarding : alighting).clone();
            end=target(); travel=mode();
        }
        return request.route(start[0],start[1],end[0],end[1],travel);
    }
    synchronized double[] target() { return (phase==Phase.FIRST_MILE ? boarding : phase==Phase.TRANSIT_LEG ? alighting : finalPoint).clone(); }
    /** Complete remaining journey for the unchanged Overview formatter, separate from active geometry. */
    synchronized JSONObject overview(JSONObject current, long fetchedAt) throws Exception {
        JSONArray currentSteps=steps(current), combined=new JSONArray();
        double seconds=TransitJourney.duration(current.optString("duration")), meters=current.optDouble("distanceMeters",-1);
        for(int i=0;i<currentSteps.length();i++) combined.put(currentSteps.getJSONObject(i));
        if(phase==Phase.FIRST_MILE && initialTransit!=null) {
            JSONArray planned=steps(initialTransit); int first=-1,last=-1;
            for(int i=0;i<planned.length();i++)if("TRANSIT".equals(planned.getJSONObject(i).optString("travelMode"))){if(first<0)first=i;last=i;}
            double railSeconds=TransitJourney.duration(initialTransit.optString("duration")),railMeters=initialTransit.optDouble("distanceMeters",-1);
            for(int i=0;i<planned.length();i++) {
                JSONObject step=planned.getJSONObject(i);
                if(i>=first && i<=last)combined.put(step);
                else { double s=TransitJourney.duration(step.optString("staticDuration"));
                    railSeconds=s<0||railSeconds<0?-1:Math.max(0,railSeconds-s);
                    double d=step.optDouble("distanceMeters",-1); railMeters=d<0||railMeters<0?-1:Math.max(0,railMeters-d); }
            }
            seconds=seconds<0||railSeconds<0?-1:seconds+railSeconds; meters=meters<0||railMeters<0?-1:meters+railMeters;
        }
        if(phase!=Phase.LAST_MILE) {
            if(egressPreview==null) {seconds=-1;meters=-1;}
            else {
                JSONArray tail=steps(egressPreview);for(int i=0;i<tail.length();i++)combined.put(tail.getJSONObject(i));
                double s=TransitJourney.duration(egressPreview.optString("duration")),d=egressPreview.optDouble("distanceMeters",-1);
                seconds=s<0||seconds<0?-1:seconds+s;meters=d<0||meters<0?-1:meters+d;
            }
        }
        JSONObject whole=new JSONObject().put("polyline",current.getJSONObject("polyline"))
                .put("legs",new JSONArray().put(new JSONObject().put("steps",combined)));
        JSONArray maneuvers=new JSONArray();
        for(int i=0;i<combined.length();i++) {
            JSONObject instruction=combined.getJSONObject(i).optJSONObject("navigationInstruction");
            if(instruction!=null)maneuvers.put(new JSONObject().put("instruction",instruction.optString("instructions")));
        }
        JSONObject progress=new JSONObject();
        if(phase==Phase.TRANSIT_LEG) {
            TransitJourney journey=TransitJourney.parse(destination,whole,fetchedAt);
            progress=new JourneyProgress(journey).snapshot(Double.NaN,Double.NaN,-1,0,fetchedAt,new JSONArray());
        }
        return NavigationOverviewPayload.create(TransitJourney.parse(destination,whole,fetchedAt),seconds,meters,fetchedAt,maneuvers,progress)
                .put("phase",phase.name()).put("transitRefreshPending",phase==Phase.FIRST_MILE);
    }
    private static JSONArray steps(JSONObject route) throws Exception {
        JSONArray out=new JSONArray(),legs=route.getJSONArray("legs");
        for(int i=0;i<legs.length();i++){JSONArray steps=legs.getJSONObject(i).getJSONArray("steps");for(int j=0;j<steps.length();j++)out.put(steps.getJSONObject(j));}
        return out;
    }
    synchronized boolean advance(String action) {
        if (!ready) return false;
        if (phase==Phase.FIRST_MILE && "board".equals(action)) phase=Phase.TRANSIT_LEG;
        else if (phase==Phase.TRANSIT_LEG && "alight".equals(action)) phase=Phase.LAST_MILE;
        else return false;
        ready=false; revision++; advancedAt=System.currentTimeMillis(); return true;
    }
    synchronized String geofence(double lat,double lng,double accuracy,long sample,long now) {
        if (!ready||phase==Phase.LAST_MILE||!Double.isFinite(accuracy)||accuracy<0||accuracy>70||sample<=advancedAt||sample<=0||now<sample||now-sample>30000) return "";
        double[] p=phase==Phase.FIRST_MILE?boarding:alighting;
        if (p==null||!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180) return "";
        double a=Math.sin(Math.toRadians(lat-p[0])/2),b=Math.sin(Math.toRadians(lng-p[1])/2);
        double h=a*a+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(p[0]))*b*b;
        return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)))<=70 ? (phase==Phase.FIRST_MILE?"board":"alight") : "";
    }
}
