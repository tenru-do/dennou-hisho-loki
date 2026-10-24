package com.example.rokidgeminisecretary;
import org.json.*;
/** Observation only. No request replay, route changes, or inferred answer body delivery. */
public final class StabilityCoherence {
 public static JSONObject compare(JSONArray ledger,JSONObject workspace,JSONObject expected,long now)throws Exception{
 boolean fresh=workspace.optBoolean("connected")&&workspace.optLong("receivedAt")>0&&now>=workspace.optLong("receivedAt")&&now-workspace.optLong("receivedAt")<=30000;
 String conversation="IDLE";JSONObject pending=null;for(int i=ledger.length()-1;i>=0;i--){JSONObject row=ledger.optJSONObject(i);if(row!=null&&row.optBoolean("tracked")&&!TrackedWebRequest.terminal(row.optString("requestState"))){pending=row;break;}}
 if(pending!=null){JSONObject remote=workspace.optJSONObject("conversation");conversation=!fresh?"STALE":remote==null||!TrackedWebRequest.validId(remote.optString("id"))?"NO_STATE":!pending.optString("key").equals(remote.optString("id"))?"MISMATCH":remote.optString("phase").equals("ANSWERED")?"BODY_CHECK_REQUIRED":remote.optString("phase").equals("RUNNING")&&pending.optString("requestState").equals("RUNNING")?"MATCH":"PHASE_DIFFERENCE";}
 if(pending==null){JSONObject remote=workspace.optJSONObject("conversation");if(!fresh)conversation="STALE";else if(remote!=null&&remote.optString("phase").equals("RUNNING"))conversation="REMOTE_RUNNING";}
 JSONObject echoed=workspace.optJSONObject("stabilityNavigation");String nav=expected.optString("id").isEmpty()?"NO_STATE":!fresh?"STALE":echoed==null?"NO_STATE":!expected.optBoolean("active")&&!echoed.optBoolean("active")?"IDLE":echoed.optLong("ageMs",-1)<0?"NO_STATE":echoed.optLong("ageMs")+now-workspace.optLong("receivedAt")>30000?"STALE":expected.optString("id").equals(echoed.optString("id"))&&expected.optBoolean("active")==echoed.optBoolean("active")?"MATCH":"MISMATCH";
 return new JSONObject().put("conversation",conversation).put("navigation",nav).put("fresh",fresh).put("automaticReplay",false);
 }
 static String label(String state){switch(state){case "MATCH":return "同じ対象の新しい状態を受信";case "IDLE":return "照合が必要な処理なし";case "STALE":return "受信状態が古いため未確認";case "MISMATCH":return "端末間で対象が異なります。現在の画面を確認";case "PHASE_DIFFERENCE":return "同じ依頼ですが進行状態が異なります。送信状況を確認";case "REMOTE_RUNNING":return "グラスで処理中。スマホ依頼との照合対象外";case "BODY_CHECK_REQUIRED":return "グラスに完了記録あり。スマホの本文保存を確認";default:return "照合に必要な状態が未受信";}}
 private StabilityCoherence(){}
}
