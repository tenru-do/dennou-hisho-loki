package com.example.rokidgeminisecretary;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class JourneyProgressTest {
    private JourneyProgress progress() throws Exception {
        // Valid Google example polyline. Use a point on its first segment.
        JSONObject step = new JSONObject().put("travelMode", "TRANSIT")
                .put("polyline", new JSONObject().put("encodedPolyline", "_p~iF~ps|U_ulLnnqC_mqNvxq`@"));
        JSONObject route = new JSONObject().put("polyline", step.getJSONObject("polyline"))
                .put("legs", new JSONArray().put(new JSONObject().put("steps", new JSONArray().put(step))));
        return new JourneyProgress(TransitJourney.parse("destination", route, 1));
    }
    @Test public void requiresNewGpsEvidenceAndExpiresConfidence() throws Exception {
        JourneyProgress p=progress(); JSONArray full=new JSONArray();
        assertFalse(p.snapshot(38.5,-120.2,5,1000,1000,full).optBoolean("positionConfirmed"));
        assertFalse(p.snapshot(38.5,-120.2,5,1000,10000,full).optBoolean("positionConfirmed"));
        JSONObject active=p.snapshot(38.5,-120.2,5,6000,10000,full);
        assertTrue(active.getBoolean("positionConfirmed"));
        assertEquals("TRANSIT", active.getJSONObject("currentLeg").getString("travelMode"));
        assertEquals(3,active.getJSONObject("currentLeg").getJSONArray("route").length());
        assertFalse(p.snapshot(38.5,-120.2,5,6000,40000,full).getBoolean("positionConfirmed"));
    }
    @Test public void distantLocationDoesNotSelectAnyLeg() throws Exception {
        JSONObject out=progress().snapshot(0,0,5,1000,1000,new JSONArray());
        assertEquals(-1,out.getInt("currentLegIndex"));
        assertFalse(out.has("currentLeg"));
    }
    @Test public void timetableIncludesEgressAndKeepsFinalTarget() throws Exception {
        String polyline="_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        JSONObject ride=new JSONObject().put("travelMode","TRANSIT").put("staticDuration","600s")
                .put("polyline",new JSONObject().put("encodedPolyline",polyline))
                .put("transitDetails",new JSONObject().put("stopCount",4).put("stopDetails",
                        new JSONObject().put("arrivalTime","2026-10-02T01:10:00Z")
                                .put("arrivalStop",new JSONObject().put("name","乗換駅"))));
        JSONObject walk=new JSONObject().put("travelMode","WALK").put("staticDuration","300s")
                .put("polyline",new JSONObject().put("encodedPolyline","??"));
        JSONObject route=new JSONObject().put("polyline",ride.getJSONObject("polyline"))
                .put("legs",new JSONArray().put(new JSONObject().put("steps",new JSONArray().put(ride).put(walk))));
        long now=java.time.Instant.parse("2026-10-02T01:00:00Z").toEpochMilli();
        JourneyProgress progress=new JourneyProgress(TransitJourney.parse("最終目的地",route,now));
        progress.snapshot(38.5,-120.2,5,now,now,new JSONArray());
        JSONObject result=progress.snapshot(38.5,-120.2,5,now+6000,now+6000,new JSONArray());
        assertTrue(result.getBoolean("positionConfirmed"));
        assertEquals("最終目的地",result.getString("finalDestination"));
        assertEquals("2026-10-02T01:15:00Z",result.getString("finalArrivalTime"));
        assertEquals(894,result.getDouble("totalRemainingSeconds"),0.1);
        assertEquals(594,result.getJSONObject("currentLeg").getDouble("remainingSeconds"),0.1);
        assertEquals(4,result.getJSONObject("currentLeg").getInt("stopCount"));
    }
    @Test public void progressesPastStationThroughTransfersToEgress() throws Exception {
        JSONArray steps=new JSONArray();
        String[] modes={"WALK","TRANSIT","WALK","TRANSIT","WALK"};
        for(int i=0;i<modes.length;i++) steps.put(new JSONObject().put("travelMode",modes[i])
                .put("polyline",new JSONObject().put("encodedPolyline",segment(3500000+i*1000,13900000))));
        JSONObject route=new JSONObject().put("polyline",steps.getJSONObject(0).getJSONObject("polyline"))
                .put("legs",new JSONArray().put(new JSONObject().put("steps",steps)));
        JourneyProgress p=new JourneyProgress(TransitJourney.parse("最終目的地",route,1000));
        String[] kinds={"ACCESS","RIDE","TRANSFER","RIDE","EGRESS"};
        for(int i=0;i<kinds.length;i++) {
            long t=1000+i*20000;
            double lat=35+i*0.01+0.005;
            p.snapshot(lat,139,5,t,t,new JSONArray());
            JSONObject active=p.snapshot(lat,139,5,t+6000,t+6000,new JSONArray());
            assertTrue(active.getBoolean("positionConfirmed"));
            assertEquals(i,active.getInt("currentLegIndex"));
            assertEquals(kinds[i],active.getJSONObject("currentLeg").getString("kind"));
            assertEquals("最終目的地",active.getString("finalDestination"));
        }
    }
    private String segment(int lat,int lng) {
        return encoded(lat)+encoded(lng)+encoded(1000)+encoded(0);
    }
    private String encoded(int delta) {
        int value=delta<0?~(delta<<1):delta<<1;
        StringBuilder text=new StringBuilder();
        while(value>=32){text.append((char)((32|(value&31))+63));value>>=5;}
        return text.append((char)(value+63)).toString();
    }
}
