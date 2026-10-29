package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
final class EnergySampleTime {
 static long at(JSONObject sample){long seen=sample.optLong("observedAt");if(!"HEALBE_SDK".equals(sample.optString("source")))return seen;long measured=sample.optLong("measuredAt");return measured>0&&seen>=measured?measured:0;}
}
