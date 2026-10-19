package com.example.rokidkeyboardbridge;
import org.json.*;
/** A browser turn has an opaque ID; never infer identity by matching the body. */
final class TrackedWebRequest {
 static final String PREFIX="__LOKI_WEB_REQUEST__:";
 static boolean validId(String id){return id!=null&&id.matches("[a-f0-9]{64}");}
 static String wire(String id,String text){if(!validId(id)||text==null||text.trim().isEmpty()||text.length()>2800||text.contains("__LOKI_")||text.contains("__CODEX_"))throw new IllegalArgumentException("Invalid tracked request");return PREFIX+id+":"+text;}
 static String wire(String id,String text,String referenceKey){
  wire(id,text);if(referenceKey==null||referenceKey.isEmpty())return wire(id,text);
  if(!referenceKey.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid reference key");
  return PREFIX+id+":__LOKI_REF_KEY__:"+referenceKey+":"+text;
 }
 static JSONObject parse(String wire)throws Exception{
  if(wire==null||!wire.startsWith(PREFIX)||wire.length()<PREFIX.length()+66)throw new IllegalArgumentException("Invalid envelope");
  String id=wire.substring(PREFIX.length(),PREFIX.length()+64);if(wire.charAt(PREFIX.length()+64)!=':')throw new IllegalArgumentException("Invalid delimiter");
  String text=wire.substring(PREFIX.length()+65),referenceKey="";String marker="__LOKI_REF_KEY__:";
  if(text.startsWith(marker)){if(text.length()<marker.length()+66||text.charAt(marker.length()+64)!=':')throw new IllegalArgumentException("Invalid reference envelope");referenceKey=text.substring(marker.length(),marker.length()+64);text=text.substring(marker.length()+65);if(!referenceKey.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid reference key");}
  wire(id,text);return new JSONObject().put("id",id).put("text",text).put("referenceKey",referenceKey).put("phase","READY");
 }
 static boolean executionGrant(String before,String event,String after){return "RECEIVED".equals(before)&&"RUNNING".equals(event)&&"RUNNING".equals(after);}
 static boolean terminal(String state){return "ANSWERED".equals(state)||"FAILED".equals(state)||"UNCONFIRMED".equals(state);}
 static JSONObject receipt(JSONObject original,String event,String reason,long now)throws Exception{
  if(!validId(original.optString("key"))||!original.optBoolean("tracked"))throw new IllegalArgumentException("Unknown tracked request");
  if(!"RECEIVED".equals(event)&&!"RUNNING".equals(event)&&!terminal(event))throw new IllegalArgumentException("Invalid receipt event");
  if(reason==null)reason="";if(!reason.matches("|MODEL_ERROR|SAFETY_STOP|INTERRUPTED|LOCAL_RESULT|RESTART|REFERENCE_CHANGED"))throw new IllegalArgumentException("Invalid receipt reason");
  JSONObject copy=new JSONObject(original.toString());String before=copy.optString("requestState","CREATED");
  if(terminal(before)||before.equals(event)||("RUNNING".equals(before)&&"RECEIVED".equals(event)))return copy;
  if(!before.equals("CREATED")&&!before.equals("QUEUED")&&!before.equals("RECEIVED")&&!before.equals("RUNNING"))throw new IllegalArgumentException("Invalid prior state");
  copy.put("requestState",event).put("receiptAt",now).put("receiptReason",reason);return copy;
 }
 private TrackedWebRequest(){}
}
