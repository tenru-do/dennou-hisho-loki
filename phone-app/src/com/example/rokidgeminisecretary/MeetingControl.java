package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
final class MeetingControl {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("meeting_control",0);}
 static synchronized JSONObject queue(Context c,JSONObject input)throws Exception{throw new IOException(MeetingFeatureRetired.MESSAGE);}
 static synchronized JSONObject update(Context c,JSONObject state)throws Exception {
  JSONObject safe=new JSONObject().put("recording",state.optBoolean("recording")).put("sessionId",state.optString("sessionId").matches("[0-9]{13}-[a-f0-9-]{36}")?state.optString("sessionId"):"").put("mode",Math.max(0,Math.min(3,state.optInt("mode")))).put("photos",state.optBoolean("photos")).put("topic",state.optBoolean("topic")).put("status",state.optString("status").substring(0,Math.min(300,state.optString("status").length()))).put("receivedAt",System.currentTimeMillis());
  JSONObject progress=state.optJSONObject("progress");if(progress!=null){JSONObject bounded=new JSONObject();for(String key:new String[]{"capturedMs","writtenMs","sealedMs","pendingMs","elapsedMs"})bounded.put(key,Math.max(0,Math.min(7260000L,progress.optLong(key))));bounded.put("queuedFrames",Math.max(0,Math.min(64,progress.optInt("queuedFrames"))));safe.put("progress",bounded);}
  JSONObject request=new JSONObject(prefs(c).getString("request","{}"));if(request.optString("requestId").equals(state.optString("requestId"))&&!request.optString("requestId").isEmpty()){
   String phase=state.optString("requestStatus");if(phase.length()>300)phase=phase.substring(0,300);String machine=state.optString("requestPhase");if(machine.matches("RUNNING|COMPLETED|FAILED|UNCONFIRMED"))request.put("phase",machine).put("updatedAt",System.currentTimeMillis());request.put("status",phase);prefs(c).edit().putString("request",request.toString()).commit();
  }
  prefs(c).edit().putString("state",safe.toString()).commit();return new JSONObject().put("ok",true);
 }
 static synchronized JSONObject status(Context c)throws Exception {JSONObject request=new JSONObject(prefs(c).getString("request","{}"));if(request.optLong("expiresAt")<System.currentTimeMillis()&&request.optString("status").equals("送信待ち"))request.put("status","期限切れ：グラスの接続を確認して再操作してください").put("phase","UNCONFIRMED");JSONObject state=new JSONObject(prefs(c).getString("state","{}"));long age=System.currentTimeMillis()-state.optLong("receivedAt");state.put("connected",age>=0&&age<15000);return new JSONObject().put("state",state).put("request",request);}
}
