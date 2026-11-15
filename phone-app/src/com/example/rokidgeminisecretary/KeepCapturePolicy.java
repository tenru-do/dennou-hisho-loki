package com.example.rokidgeminisecretary;
import org.json.JSONObject;
/** Technical KEEP capture state only; no image, location, or conversation content. */
final class KeepCapturePolicy {
 static JSONObject safe(JSONObject in)throws Exception {
  String phase=in.optString("phase"),reason=in.optString("reason");
  if(!phase.matches("STARTING|RUNNING|STOPPED|FAILED|INTERRUPTED"))phase="UNKNOWN";
  if(!reason.matches("LOW_SPACE|SAVE_ERROR|CAPTURE_ERROR|USER_STOP|INTERRUPTED|"))reason="";
  return new JSONObject().put("phase",phase).put("reason",reason).put("at",Math.max(0,in.optLong("at"))).put("startedAt",Math.max(0,in.optLong("startedAt"))).put("saved",Math.max(0,in.optInt("saved"))).put("lastSavedAt",Math.max(0,in.optLong("lastSavedAt"))).put("availableBytes",Math.max(0,in.optLong("availableBytes")));
 }
 static String label(JSONObject in)throws Exception {
  JSONObject j=safe(in);String phase=j.getString("phase"),reason=j.getString("reason");
  String text="前回KEEP："+(phase.equals("FAILED")?"保存・撮影で停止":phase.equals("INTERRUPTED")?"処理中断（再起動など）":phase.equals("STOPPED")?"停止":phase.equals("RUNNING")?"保存記録あり":phase.equals("STARTING")?"開始要求のみ（保存未確認）":"保存状況は未取得");
  if(!phase.equals("UNKNOWN"))text+=" ／ この回の保存成功 "+j.getInt("saved")+"枚";
  if(reason.equals("LOW_SPACE"))text+=" ／ 空き容量不足（1GB以上の空きが必要）";
  else if(reason.equals("SAVE_ERROR"))text+=" ／ 写真の保存に失敗";
  else if(reason.equals("CAPTURE_ERROR"))text+=" ／ カメラ撮影に失敗";
  if(j.getLong("at")>0)text+=" ／ 記録時刻 "+new java.text.SimpleDateFormat("MM/dd HH:mm:ss",java.util.Locale.JAPAN).format(new java.util.Date(j.getLong("at")));
  return text;
 }
 private KeepCapturePolicy(){}
}
