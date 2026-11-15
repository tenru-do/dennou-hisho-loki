package com.example.rokidkeyboardbridge;
import android.content.Context;import org.json.JSONObject;
final class KeepCaptureState {
 static void record(Context c,String phase,String reason,long startedAt,int saved,long lastSavedAt,long available){try{JSONObject j=KeepCapturePolicy.safe(new JSONObject().put("phase",phase).put("reason",reason).put("at",System.currentTimeMillis()).put("startedAt",startedAt).put("saved",saved).put("lastSavedAt",lastSavedAt).put("availableBytes",available));if(!c.getSharedPreferences("keep_capture_state",0).edit().putString("last",j.toString()).commit())android.util.Log.w("LokiKeepState","state commit failed");}catch(Exception e){android.util.Log.w("LokiKeepState","state unavailable");}}
 static JSONObject snapshot(Context c){try{if(PhotoRecorderService.active!=null)return PhotoRecorderService.active.captureState();JSONObject j=KeepCapturePolicy.safe(new JSONObject(c.getSharedPreferences("keep_capture_state",0).getString("last","{}")));if(PhotoRecorderService.active==null&&j.optString("phase").matches("RUNNING|STARTING"))j.put("phase","INTERRUPTED").put("reason","INTERRUPTED");return j;}catch(Exception e){return new JSONObject();}}
 private KeepCaptureState(){}
}
