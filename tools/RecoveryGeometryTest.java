package com.example.rokidgeminisecretary;
import org.json.*;
public final class RecoveryGeometryTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static JSONObject at(double lat,double lng)throws Exception{return new JSONObject().put("latLng",new JSONObject().put("latitude",lat).put("longitude",lng));}
    public static void main(String[] args)throws Exception {
        JSONObject ride=new JSONObject().put("travelMode","TRANSIT")
            .put("startLocation",at(35.7,139.4)).put("endLocation",at(35.8,139.5));
        JSONObject walk=new JSONObject().put("travelMode","WALK")
            .put("startLocation",at(35.8,139.5)).put("endLocation",at(35.801,139.502));
        JSONObject route=new JSONObject().put("legs",new JSONArray().put(new JSONObject()
            .put("steps",new JSONArray().put(ride).put(walk))));
        GooglePolyline.mergeSteps(route);
        check(route.getJSONArray("lokiRouteSegments").length()==2,"all segments retained");
        check(ride.getBoolean("lokiGeometryEstimated"),"missing rail shape explicitly estimated");
        check(GooglePolyline.decode(ride.getJSONObject("polyline").getString("encodedPolyline")).length==2,"endpoint interpolation");
        TransitJourney journey=TransitJourney.parse("test",route,1000);
        JSONObject snapshot=new JourneyProgress(journey).snapshot(Double.NaN,Double.NaN,-1,0,1000,new JSONArray("[[35.7,139.4],[35.8,139.5],[35.801,139.502]]"));
        check(snapshot.getJSONArray("routeSegments").length()==2,"both segments reach HUD before GPS lock");
        check(!snapshot.has("currentLeg"),"no fabricated boarding");
        check(snapshot.getJSONArray("routeSegments").getJSONObject(0).getBoolean("estimated"),"estimated label retained over wire");
        String text=NewsRssFallback.plain("<p>一文です。</p><p>二文です。</p>");
        check(text.equals("一文です。\n\n二文です。"),"HTML paragraph breaks retained");
        check(TopicTextPolicy.limit(text,100).equals(text),"TOPIC sanitizer preserves line breaks");
        check(com.example.rokidkeyboardbridge.TtsSentenceBuffer.splitTopic(text).length==1,"two sentences buffered");
        check(com.example.rokidkeyboardbridge.TtsSentenceBuffer.splitTopic(text)[0].contains("\n"),"TTS TOPIC lines retained");
        System.out.println("RecoveryGeometryTest PASS: 10 checks, missing rail shape, wire segments, no GPS, TOPIC newline and TTS");
    }
}
