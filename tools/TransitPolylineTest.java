package com.example.rokidgeminisecretary;
import org.json.*;
public class TransitPolylineTest {
    public static void main(String[] args) throws Exception {
        String line = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        JSONObject step = new JSONObject().put("polyline", new JSONObject().put("encodedPolyline", line));
        JSONObject route = new JSONObject().put("legs", new JSONArray().put(new JSONObject()
                .put("steps", new JSONArray().put(step))));
        GooglePolyline.mergeSteps(route);
        if (!line.equals(route.getJSONObject("polyline").getString("encodedPolyline"))) throw new AssertionError("roundtrip");
        route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").put(new JSONObject());
        GooglePolyline.mergeSteps(route);
        if (!line.equals(route.getJSONObject("polyline").getString("encodedPolyline"))) throw new AssertionError("missing step fallback");
        JSONObject zero = new JSONObject().put("distanceMeters", 0);
        JSONObject onlySteps = new JSONObject().put("legs", new JSONArray().put(new JSONObject()
                .put("steps", new JSONArray().put(zero).put(step).put(zero))));
        GooglePolyline.mergeSteps(onlySteps);
        if (!line.equals(onlySteps.getJSONObject("polyline").getString("encodedPolyline"))) throw new AssertionError("zero distance steps");
        JSONObject broken = new JSONObject().put("polyline", new JSONObject().put("encodedPolyline", line))
                .put("legs", new JSONArray().put("invalid leg"));
        GooglePolyline.mergeStepsOrOverview(broken);
        if (!line.equals(broken.getJSONObject("polyline").getString("encodedPolyline"))) throw new AssertionError("overview fallback");
        String mask = RoutesRequestPolicy.mask("TRANSIT");
        for (String field : new String[]{"routes.polyline", "routes.legs", "routes.description", "routes.warnings"})
            if (!java.util.Arrays.asList(mask.split(",")).contains(field)) throw new AssertionError(field);
        System.out.println("TransitPolyline: 8 checks passed");
    }
}
