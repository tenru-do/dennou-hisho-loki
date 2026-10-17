package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
final class MeetingControl {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("meeting_control",0);}
 static synchronized JSONObject queue(Context c,JSONObject input)throws Exception{
  String action=input.getString("action"),id=input.getString("requestId");int mode=input.optInt("mode",0);
  if(!id.matches("[a-fA-F0-9-]{36}")||!(action.equals("start")||action.equals("stop")||action.equals("transfer"))||mode<0||mode>3)throw new IOException("操作または録音モードが不正です");
  JSONObject old=new JSONObject(prefs(c).getString("request","{}"));if(id.equals(old.optString("requestId")))return old;
  if(old.optString("status").equals("送信待ち")&&old.optLong("expiresAt")>System.currentTimeMillis())throw new IOException("前の操作の確認を待ってください");
  JSONObject request=new JSONObject().put("action",action).put("mode",mode).put("requestId",id).put("expiresAt",System.currentTimeMillis()+120000).put("status","送信待ち");
  if(!MainActivity.queueBridgeCommand(c,"__LOKI_MEETING__:"+request.toString(),true))throw new IOException("グラスへの送信待ちが満杯です");
  prefs(c).edit().putString("request",request.toString()).commit();return request;
 }
 static synchronized JSONObject update(Context c,JSONObject state)throws Exception {
  JSONObject safe=new JSONObject().put("recording",state.optBoolean("recording")).put("mode",Math.max(0,Math.min(3,state.optInt("mode")))).put("photos",state.optBoolean("photos")).put("topic",state.optBoolean("topic")).put("status",state.optString("status").substring(0,Math.min(300,state.optString("status").length()))).put("receivedAt",System.currentTimeMillis());
  JSONObject request=new JSONObject(prefs(c).getString("request","{}"));if(request.optString("requestId").equals(state.optString("requestId"))&&!request.optString("requestId").isEmpty()){
   String phase=state.optString("requestStatus");if(phase.length()>300)phase=phase.substring(0,300);request.put("status",phase);prefs(c).edit().putString("request",request.toString()).commit();
  }
  prefs(c).edit().putString("state",safe.toString()).commit();return new JSONObject().put("ok",true);
 }
 static synchronized JSONObject status(Context c)throws Exception {JSONObject request=new JSONObject(prefs(c).getString("request","{}"));if(request.optLong("expiresAt")<System.currentTimeMillis()&&request.optString("status").equals("送信待ち"))request.put("status","期限切れ：グラスの接続を確認して再操作してください");JSONObject state=new JSONObject(prefs(c).getString("state","{}"));state.put("connected",System.currentTimeMillis()-state.optLong("receivedAt")<15000);return new JSONObject().put("state",state).put("request",request);}
}
