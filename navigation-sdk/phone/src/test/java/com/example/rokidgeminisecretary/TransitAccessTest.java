package com.example.rokidgeminisecretary;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransitAccessTest {
    @Test public void notificationCannotReplaceFinalTargetOrLeakTotals() throws Exception {
        TransitAccess access = new TransitAccess("最終施設", "35.6,139.7", "bicycling");
        access.station = "出発駅"; access.fetching = false;
        JSONObject notification = new JSONObject().put("destination", "別の乗換駅").put("travelMode", "driving")
                .put("arrival", "18:00").put("totalRemainingDuration", "3分").put("journey", new JSONObject());
        JSONObject result = access.apply(notification, new JSONArray("[[35,139],[35.1,139.1]]"), "walking", 125);
        assertEquals("最終施設", result.getString("destination"));
        assertEquals("35.6,139.7", result.getString("finalDestinationCoordinates"));
        assertEquals("出発駅", result.getString("routeDestination"));
        assertEquals("transit_access", result.getString("routeTargetKind"));
        assertEquals("", result.getString("arrival"));
        assertEquals("", result.getString("totalRemainingDuration"));
        assertTrue(result.getBoolean("walkingFallback"));
        assertFalse(result.has("journey"));
        assertTrue(result.getString("accessDuration").startsWith("駅まで約3分"));
        assertEquals(2, result.getJSONArray("route").length());
    }
    @Test public void stationSearchIsBoundedAndDistanceRanked() throws Exception {
        JSONObject request = TransitAccess.stationRequest(35.7,139.4);
        assertEquals("DISTANCE",request.getString("rankPreference"));
        assertEquals(1,request.getInt("maxResultCount"));
        assertEquals(5000,request.getJSONObject("locationRestriction").getJSONObject("circle").getDouble("radius"),0);
        assertEquals("train_station",request.getJSONArray("includedTypes").getString(0));
        try { TransitAccess.stationRequest(Double.NaN,0); fail(); } catch (Exception expected) { }
    }
    @Test public void failuresKeepFinalTargetAndNeverClaimRouteReady() throws Exception {
        for (String error : new String[]{"google_station_not_found", "google_current_location_unavailable", "google_http_403"}) {
            TransitAccess access = new TransitAccess("最終施設", "35,139", "walking");
            access.fetching=false; access.error=error;
            JSONObject result=access.apply(new JSONObject(),null,"walking",-1);
            assertFalse(result.getBoolean("routeReady"));
            assertEquals("最終施設",result.getString("destination"));
            assertFalse(result.getString("instruction").isEmpty());
            assertEquals(0,result.getJSONArray("route").length());
        }
    }
}
