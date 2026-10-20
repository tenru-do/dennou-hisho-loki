package com.example.rokidgeminisecretary;
import org.json.*;
/** Explicit text-only correction; no dispatch or mutation of historical messages. */
final class CorrectionDraft {
 static JSONObject select(JSONObject recognition,JSONArray outbox,long now)throws Exception{String text="",source="";long at=0;if(recognition!=null&&recognition.optString("source").equals("VOICE")&&recognition.optString("phase").matches("FINAL|COMPLETE|COMPLETED|RECOGNIZED|ANSWERED")&&fresh(recognition.optLong("at"),now)){text=recognition.optString("text");at=recognition.optLong("at");source="音声認識";}for(int i=0;i<outbox.length();i++){JSONObject row=outbox.optJSONObject(i);if(row==null||!row.optBoolean("tracked")||!TrackedWebRequest.validId(row.optString("key")))continue;long t=row.optLong("time");if(fresh(t,now)&&t>at){at=t;text=row.optString("message");source="会話の送信本文";}}if(text.trim().isEmpty()||text.length()>2400)return new JSONObject().put("available",false);return new JSONObject().put("available",true).put("text",text).put("at",at).put("source",source);}
 static boolean fresh(long at,long now){return at>0&&now>=at&&now-at<=900000;}
 private CorrectionDraft(){}
}
