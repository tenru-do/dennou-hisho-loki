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
}
