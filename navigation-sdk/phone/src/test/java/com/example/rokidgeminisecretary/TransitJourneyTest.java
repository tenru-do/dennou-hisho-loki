package com.example.rokidgeminisecretary;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransitJourneyTest {
    private JSONObject step(String mode, String geometry) throws Exception {
        return new JSONObject().put("travelMode", mode)
                .put("polyline", new JSONObject().put("encodedPolyline", geometry));
    }
    private JSONObject route(JSONArray steps) throws Exception {
        return new JSONObject().put("polyline", new JSONObject().put("encodedPolyline", "whole"))
                .put("legs", new JSONArray().put(new JSONObject().put("steps", steps)));
    }
    @Test public void keepsWholeTripAndWalkTrainTransferBusWalk() throws Exception {
        JSONObject train = step("TRANSIT", "rail").put("transitDetails", new JSONObject()
                .put("stopDetails", new JSONObject()
                    .put("departureStop", new JSONObject().put("name", "A駅"))
                    .put("arrivalStop", new JSONObject().put("name", "B駅"))
                    .put("departureTime", "2026-09-27T00:00:00Z")
                    .put("arrivalTime", "2026-09-27T00:30:00Z"))
                .put("transitLine", new JSONObject().put("name", "テスト線")
                    .put("vehicle", new JSONObject().put("type", "HEAVY_RAIL"))));
        TransitJourney journey = TransitJourney.parse("最終目的地", route(new JSONArray()
                .put(step("WALK", "access")).put(train).put(step("WALK", "transfer"))
                .put(step("TRANSIT", "bus").put("transitDetails", new JSONObject()
                    .put("transitLine", new JSONObject().put("name", "テストバス")
                        .put("vehicle", new JSONObject().put("type", "BUS")))))
                .put(step("WALK", "egress"))), 123);
        assertEquals("最終目的地", journey.finalDestination);
        assertEquals("whole", journey.encodedPolyline);
        assertEquals(5, journey.legs.size());
        assertEquals("ACCESS", journey.legs.get(0).kind);
        assertEquals("RIDE", journey.legs.get(1).kind);
        assertEquals("TRANSFER", journey.legs.get(2).kind);
        assertEquals("EGRESS", journey.legs.get(4).kind);
        assertEquals("rail", journey.legs.get(1).encodedPolyline);
        assertEquals("A駅", journey.legs.get(1).departureStop);
        assertEquals("B駅", journey.legs.get(1).arrivalStop);
        assertEquals("テスト線", journey.legs.get(1).lineName);
        assertEquals("2026-09-27T00:30:00Z", journey.legs.get(1).arrivalTime);
        assertEquals("bus", journey.legs.get(3).encodedPolyline);
        assertEquals("BUS", new JSONObject(journey.legs.get(3).transitDetailsJson)
                .getJSONObject("transitLine").getJSONObject("vehicle").getString("type"));
        train.getJSONObject("transitDetails").put("changed", true);
        assertFalse(journey.legs.get(1).transitDetailsJson.contains("changed"));
    }
    @Test public void missingOptionalGeometryIsNotInvented() throws Exception {
        TransitJourney j = TransitJourney.parse("目的地", route(new JSONArray()
                .put(new JSONObject().put("travelMode", "WALK"))), 1);
        assertEquals("", j.legs.get(0).encodedPolyline);
        assertEquals("{}", j.legs.get(0).transitDetailsJson);
    }
    @Test(expected = JSONException.class) public void rejectsEmptyTrip() throws Exception {
        TransitJourney.parse("目的地", route(new JSONArray()), 1);
    }
    @Test(expected = UnsupportedOperationException.class) public void immutableLegList() throws Exception {
        TransitJourney.parse("目的地", route(new JSONArray().put(step("WALK", "a"))), 1).legs.clear();
    }
}
