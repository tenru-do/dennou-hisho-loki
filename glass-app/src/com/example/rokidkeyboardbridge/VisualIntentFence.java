package com.example.rokidkeyboardbridge;
import org.json.*;
/** A view-session revision, never wall-clock ordering between different devices. */
final class VisualIntentFence {
 private final String session=java.util.UUID.randomUUID().toString();private volatile long revision;
 void changed(){revision++;}
 JSONObject snapshot(){try{return new JSONObject().put("session",session).put("revision",revision);}catch(JSONException impossible){throw new IllegalStateException(impossible);}}
 boolean accepts(JSONObject base){return base!=null&&session.equals(base.optString("session"))&&base.has("revision")&&base.optLong("revision",-1)==revision;}
 long revision(){return revision;}
}
