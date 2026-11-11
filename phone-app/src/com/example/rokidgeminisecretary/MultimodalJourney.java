package com.example.rokidgeminisecretary;

import org.json.*;

/** Session-local plan. Route publication and phase advancement are separate transactions. */
final class MultimodalJourney {
    enum Phase { FIRST_MILE, BOARDING, TRANSIT_LEG, LAST_MILE }
    final String destination, accessMode, egressMode;
    final double[] finalPoint;
    volatile Phase phase = Phase.FIRST_MILE;
    double[] boarding, alighting;
    String boardingName = "", alightingName = "";
    JSONObject initialTransit, egressPreview, accessPreview;
    int manualTransitIndex=-1;
    private JourneyProgress initialProgress;
    boolean boardingEstimated;
    final String sessionId=java.util.UUID.randomUUID().toString();
    volatile boolean ready;
    long advancedAt;
    long revision;
    MultimodalJourney(String destination, double[] finalPoint, String access, String egress) {
        this.destination = destination; this.finalPoint = finalPoint.clone();
        accessMode = "bicycling".equals(access) ? "bicycling" : "walking";
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
            // Keep the selected provider/route through boarding. A fresh station-to-station
            // Google query can select another train or fail where the original provider works.
            if ((phase==Phase.BOARDING||phase==Phase.TRANSIT_LEG) && initialTransit!=null) return transitSection(initialTransit);
            start=phase==Phase.FIRST_MILE ? new double[]{lat,lng} : (phase==Phase.TRANSIT_LEG ? boarding : alighting).clone();
            end=target(); travel=mode();
        }
        JSONObject road=request.route(start[0],start[1],end[0],end[1],travel);
        synchronized(this){if(phase==Phase.FIRST_MILE)accessPreview=new JSONObject(road.toString());}
        return road;
    }
    static JSONObject transitSection(JSONObject selected) throws Exception {
        JSONArray all=steps(selected), kept=new JSONArray(); int first=-1,last=-1;
        for(int i=0;i<all.length();i++)if("TRANSIT".equals(all.getJSONObject(i).optString("travelMode"))){if(first<0)first=i;last=i;}
        if(first<0)throw new Exception("google_transit_stations_missing");
        double seconds=0,meters=0;
        for(int i=first;i<=last;i++){JSONObject s=new JSONObject(all.getJSONObject(i).toString());kept.put(s);
            double d=TransitJourney.duration(s.optString("staticDuration"));seconds=seconds<0||d<0?-1:seconds+d;
            d=s.optDouble("distanceMeters",-1);meters=meters<0||d<0?-1:meters+d;}
        JSONObject route=new JSONObject(selected.toString()).put("legs",new JSONArray().put(new JSONObject().put("steps",kept)))
            .put("distanceMeters",meters).put("duration",seconds+"s");
        route.remove("polyline");GooglePolyline.mergeSteps(route);
        if(!route.has("polyline"))throw new Exception("google_route_geometry_unavailable");
        return route;
    }
    synchronized double[] target() { return (phase==Phase.FIRST_MILE||phase==Phase.BOARDING ? boarding : phase==Phase.TRANSIT_LEG ? alighting : finalPoint).clone(); }
    /** Being near the selected platform is evidence of arrival, never proof of boarding. */
    synchronized boolean startAtBoardingStop(double lat,double lng,double accuracy,long sample,long now) {
        if(phase!=Phase.FIRST_MILE||!validFix(lat,lng,accuracy,sample,now)||boarding==null||distance(lat,lng,boarding)>70)return false;
        phase=Phase.BOARDING;revision++;advancedAt=now;return true;
    }
    private static boolean validFix(double lat,double lng,double accuracy,long sample,long now){return Double.isFinite(lat)&&Double.isFinite(lng)&&Math.abs(lat)<=90&&Math.abs(lng)<=180&&Double.isFinite(accuracy)&&accuracy>=0&&accuracy<=70&&sample>0&&now>=sample&&now-sample<=30000;}
    private static double distance(double lat,double lng,double[] p){double a=Math.sin(Math.toRadians(lat-p[0])/2),b=Math.sin(Math.toRadians(lng-p[1])/2);double h=a*a+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(p[0]))*b*b;return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));}
    /** Complete remaining journey for the unchanged Overview formatter, separate from active geometry. */
    synchronized JSONObject overview(JSONObject current, long fetchedAt) throws Exception {
        if(phase==Phase.FIRST_MILE&&accessPreview==null)accessPreview=new JSONObject(current.toString());
        if(phase==Phase.LAST_MILE)egressPreview=new JSONObject(current.toString());
        JSONArray combined=new JSONArray(); double seconds=0,meters=0;
        JSONObject[] sections=initialTransit==null?new JSONObject[]{current}:new JSONObject[]{accessPreview,transitSection(initialTransit),egressPreview};
        for(JSONObject section:sections){if(section==null){seconds=-1;meters=-1;continue;}JSONArray rows=steps(section);for(int i=0;i<rows.length();i++)combined.put(rows.getJSONObject(i));double t=TransitJourney.duration(section.optString("duration")),d=section.optDouble("distanceMeters",-1);seconds=seconds<0||t<0?-1:seconds+t;meters=meters<0||d<0?-1:meters+d;}
        JSONObject whole=new JSONObject().put("legs",new JSONArray().put(new JSONObject().put("steps",combined)));
        GooglePolyline.mergeSteps(whole);if(!whole.has("polyline"))whole.put("polyline",current.getJSONObject("polyline"));
        JSONArray maneuvers=new JSONArray();
        for(int i=0;i<combined.length();i++) {
            JSONObject instruction=combined.getJSONObject(i).optJSONObject("navigationInstruction");
            if(instruction!=null)maneuvers.put(new JSONObject().put("instruction",instruction.optString("instructions")));
        }
        JSONObject progress=new JSONObject();
        if(phase==Phase.BOARDING||phase==Phase.TRANSIT_LEG) {
            TransitJourney journey=TransitJourney.parse(destination,whole,fetchedAt);
            progress=new JourneyProgress(journey).snapshot(Double.NaN,Double.NaN,-1,0,fetchedAt,new JSONArray());
        }
        return NavigationOverviewPayload.create(TransitJourney.parse(destination,whole,fetchedAt),seconds,meters,fetchedAt,maneuvers,progress)
                .put("phase",phase.name()).put("transitRefreshPending",phase==Phase.FIRST_MILE)
                .put("requestedMode","transit").put("wholeRoute",routePoints(whole));
    }
    private static JSONArray steps(JSONObject route) throws Exception {
        JSONArray out=new JSONArray(),legs=route.getJSONArray("legs");
        for(int i=0;i<legs.length();i++){JSONArray steps=legs.getJSONObject(i).getJSONArray("steps");for(int j=0;j<steps.length();j++)out.put(steps.getJSONObject(j));}
        return out;
    }
    private static JSONArray routePoints(JSONObject route)throws Exception {JSONArray out=new JSONArray();for(double[] p:GooglePolyline.decode(route.getJSONObject("polyline").getString("encodedPolyline")))out.put(new JSONArray().put(p[0]).put(p[1]));return out;}
    synchronized JSONArray boardingChoices() throws Exception {
        JSONArray out=new JSONArray();if(!ready||initialTransit==null||phase==Phase.LAST_MILE)return out;
        JSONArray rows=steps(transitSection(initialTransit));
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!"TRANSIT".equals(row.optString("travelMode"))||i<manualTransitIndex)continue;JSONObject d=row.getJSONObject("transitDetails"),stops=d.getJSONObject("stopDetails"),line=d.optJSONObject("transitLine");String label=(line==null?"公共交通":line.optString("name","公共交通"))+" ／ "+stops.getJSONObject("departureStop").optString("name")+" → "+stops.getJSONObject("arrivalStop").optString("name");out.put(new JSONObject().put("index",i).put("label",label));}return out;
    }
    synchronized boolean board(int index){try{JSONArray choices=boardingChoices();if(index<0&&choices.length()==1)index=choices.getJSONObject(0).getInt("index");for(int i=0;i<choices.length();i++)if(index==choices.getJSONObject(i).getInt("index")){manualTransitIndex=index;boardingEstimated=false;phase=Phase.TRANSIT_LEG;ready=false;revision++;advancedAt=System.currentTimeMillis();return true;}}catch(Exception ignored){}return false;}
    synchronized int inferRide(double lat,double lng,double accuracy,double speed,long sample,long now){
        if(!ready||initialTransit==null||phase==Phase.LAST_MILE||phase==Phase.TRANSIT_LEG||!validFix(lat,lng,accuracy,sample,now)||!Double.isFinite(speed)||speed<4||boarding==null||distance(lat,lng,boarding)<200)return -1;
        try{if(initialProgress==null)initialProgress=new JourneyProgress(TransitJourney.parse(destination,transitSection(initialTransit),now));JSONObject snap=initialProgress.snapshot(lat,lng,accuracy,sample,now,new JSONArray());JSONObject leg=snap.optJSONObject("currentLeg");return snap.optBoolean("positionConfirmed")&&leg!=null&&"TRANSIT".equals(leg.optString("travelMode"))?snap.optInt("currentLegIndex",-1):-1;}catch(Exception unavailable){return -1;}
    }
    synchronized boolean advance(String action) {
        if (!ready) return false;
        if ((phase==Phase.FIRST_MILE||phase==Phase.BOARDING) && "board".equals(action)) phase=Phase.TRANSIT_LEG;
        else if(phase==Phase.FIRST_MILE && "arrive".equals(action))phase=Phase.BOARDING;
        else if (phase==Phase.TRANSIT_LEG && "alight".equals(action)) {
            boolean next=false;
            if(manualTransitIndex>=0&&initialTransit!=null)try{JSONArray rows=steps(transitSection(initialTransit));for(int i=manualTransitIndex+1;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!"TRANSIT".equals(row.optString("travelMode")))continue;JSONObject stop=row.getJSONObject("transitDetails").getJSONObject("stopDetails").getJSONObject("departureStop");boarding=stopPoint(stop,row.getJSONObject("startLocation"));boardingName=stop.optString("name");manualTransitIndex=i;next=true;break;}}catch(Exception missing){return false;}
            phase=next?Phase.BOARDING:Phase.LAST_MILE;
        }
        else return false;
        ready=false; revision++; advancedAt=System.currentTimeMillis(); return true;
    }
    synchronized String geofence(double lat,double lng,double accuracy,long sample,long now) {
        if (!ready||phase==Phase.LAST_MILE||phase==Phase.BOARDING||!validFix(lat,lng,accuracy,sample,now)||sample<=advancedAt) return "";
        double[] p=phase==Phase.FIRST_MILE?boarding:alighting;
        if (p==null||!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180) return "";
        double a=Math.sin(Math.toRadians(lat-p[0])/2),b=Math.sin(Math.toRadians(lng-p[1])/2);
        double h=a*a+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(p[0]))*b*b;
        return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)))<=70 ? (phase==Phase.FIRST_MILE?"arrive":"alight") : "";
    }
}
