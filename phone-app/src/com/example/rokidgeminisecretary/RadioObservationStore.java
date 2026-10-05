package com.example.rokidgeminisecretary;

import org.json.JSONObject;

/** One requested observation, held only in RAM and bound to a settings generation. */
final class RadioObservationStore {
    private static String requestId = "";
    private static long revision, deadline;
    private static JSONObject result;
    private static String completionId="", completionState="unknown", completionError="";
    static synchronized void finishAnalysis(String id, String error) {
        if(!id.equals(completionId))return; completionError=error;completionState=error.isEmpty()?"completed":"error";
    }
    static synchronized JSONObject analysisStatus(String id) throws Exception {
        boolean known=id.equals(completionId)&&!id.isEmpty();
        return new JSONObject().put("known",known).put("state",known?completionState:"unknown").put("error",known?completionError:"");
    }
    static synchronized void begin(String id, long rev) {
        requestId = id; revision = rev; deadline = System.currentTimeMillis() + 25000L; result = null;
        completionId=id;completionState="capturing";completionError="";
    }
    static synchronized void clear() { requestId = ""; result = null; deadline = 0; }
    static synchronized boolean accept(JSONObject value, long currentRevision, boolean enabled) {
        if (!enabled || currentRevision != revision || System.currentTimeMillis() > deadline
                || requestId.isEmpty() || !requestId.equals(value.optString("requestId"))
                || revision != value.optLong("revision") || result != null) return false;
        String audio = value.optString("audio"), image = value.optString("image");
        if (audio.length() > 180000 || image.length() > 350000
                || !audio.matches("[A-Za-z0-9+/=]*") || !image.matches("[A-Za-z0-9+/=]*")) return false;
        result = value; completionState="analyzing";
        return true;
    }
    static synchronized JSONObject take(String id) {
        if (!requestId.equals(id) || System.currentTimeMillis() > deadline) return null;
        JSONObject value = result;
        if (value != null) clear();
        return value;
    }
}
