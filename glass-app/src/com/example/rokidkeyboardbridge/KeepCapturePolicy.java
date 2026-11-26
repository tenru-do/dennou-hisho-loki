package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
/** Technical KEEP capture state only; no image, location, or conversation content. */
final class KeepCapturePolicy {
 static JSONObject safe(JSONObject in)throws Exception {
  String phase=in.optString("phase"),reason=in.optString("reason");
  if(!phase.matches("STARTING|RUNNING|STOPPED|FAILED|INTERRUPTED"))phase="UNKNOWN";
  if(!reason.matches("LOW_SPACE|SAVE_ERROR|CAPTURE_ERROR|USER_STOP|INTERRUPTED|"))reason="";
  return new JSONObject().put("phase",phase).put("reason",reason).put("at",Math.max(0,in.optLong("at"))).put("startedAt",Math.max(0,in.optLong("startedAt"))).put("saved",Math.max(0,in.optInt("saved"))).put("lastSavedAt",Math.max(0,in.optLong("lastSavedAt"))).put("availableBytes",Math.max(0,in.optLong("availableBytes")));
 }
 static String menuCounts(JSONObject state,boolean recording) {
  String phase=state.optString("phase","UNKNOWN");
  String session=recording?"この回":"前回";
  int saved=state.optInt("saved",-1);
  int retained=state.optInt("retainedPhotos",-1);
  String capture=phase.equals("UNKNOWN")||saved<0?"未確認":saved+"枚（保存成功）";
  String current=retained<0?"確認できません":retained+"枚";
  return session+"の撮影累計 "+capture+"\nグラスの保存写真（確認時点） "+current;
 }
 private KeepCapturePolicy(){}
}
