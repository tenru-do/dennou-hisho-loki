package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;
/** Real encrypted workspace and queue, but isolated UID and synthetic commands only. */
public final class VisualRevisionPhoneChecks {
 static int n;static void check(boolean v){n++;if(!v)throw new AssertionError("phone revision "+n);}
 public static int run(Context c)throws Exception{
  n=0;String session="00000000-0000-4000-8000-000000000001";
  ConversationWorkspace.accept(c,new JSONObject().put("at",System.currentTimeMillis()).put("visualControl",new JSONObject().put("session",session).put("revision",4)));
  check(ConversationWorkspace.snapshot(c).getJSONObject("visualControl").getLong("revision")==4);
  String request="00000000-0000-4000-8000-000000000002";check(LokiWebConsole.queueReferenceConversation(c,request,"座って",""));
  JSONArray queued=MainActivity.queuedWebOrder(c);check(queued.length()==1);String id=queued.getJSONObject(0).getString("id");
  ConversationWorkspace.accept(c,new JSONObject().put("at",System.currentTimeMillis()).put("visualControl",new JSONObject().put("session",session).put("revision",5)));
  JSONObject received=LokiWebConsole.trackedReceipt(c,new JSONObject().put("id",id).put("event","RECEIVED"));check(received.getJSONObject("visualControl").getLong("revision")==4);
  JSONObject running=LokiWebConsole.trackedReceipt(c,new JSONObject().put("id",id).put("event","RUNNING"));check(running.optBoolean("execute"));check(running.getJSONObject("visualControl").getLong("revision")==4);
  JSONObject superseded=LokiWebConsole.trackedReceipt(c,new JSONObject().put("id",id).put("event","SUPERSEDED").put("reason","INTERRUPTED"));check("SUPERSEDED".equals(superseded.getString("requestState")));
  JSONObject retry=LokiWebConsole.trackedReceipt(c,new JSONObject().put("id",id).put("event","RUNNING"));check(!retry.optBoolean("execute"));
  ConversationWorkspace.accept(c,new JSONObject().put("at",System.currentTimeMillis()).put("visualControl",new JSONObject().put("session","invalid").put("revision",-1)));check(!ConversationWorkspace.snapshot(c).has("visualControl"));
  return n;
 }
}
