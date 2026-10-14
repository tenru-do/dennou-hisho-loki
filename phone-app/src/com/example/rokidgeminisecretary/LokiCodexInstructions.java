package com.example.rokidgeminisecretary;
import android.content.*;
import org.json.*;

/** Called only after the existing bridge-token authentication. */
final class LokiCodexInstructions {
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences("loki_codex_instructions",0);}
 static synchronized JSONObject handle(Context c,String path,String body)throws Exception {
  SharedPreferences p=prefs(c);long now=System.currentTimeMillis();
  JSONObject target=new JSONObject(p.getString("target","{}"));
  if(path.equals("register")){
   JSONObject in=new JSONObject(body);String id=in.getString("threadId"),title=in.getString("title");
   if(!id.matches("[a-fA-F0-9-]{36}")||title.isEmpty()||title.length()>300||title.matches("(?s).*[\\x00-\\x1f].*"))throw new IllegalArgumentException("invalid_target");
   target=new JSONObject().put("threadId",id).put("title",title).put("heartbeatAt",now);
   if(!p.edit().putString("target",target.toString()).commit())throw new IllegalStateException("save_failed");
   return new JSONObject().put("ok",true);
  }
  if(path.equals("target"))return target.put("online",now-target.optLong("heartbeatAt",0)>=0&&now-target.optLong("heartbeatAt",0)<20000);
  JSONArray queue=new JSONArray(p.getString("queue","[]"));
  if(path.equals("submit")){
   JSONObject in=new JSONObject(body);String id=in.getString("requestId"),text=in.getString("text").trim();
   if(!id.matches("[a-fA-F0-9-]{36}")||text.isEmpty()||text.length()>3000||text.indexOf(0)>=0)throw new IllegalArgumentException("invalid_instruction");
   long age=now-target.optLong("heartbeatAt",0);
   if(age<0||age>=20000||!in.getString("threadId").equals(target.optString("threadId"))||!in.getString("title").equals(target.optString("title")))throw new IllegalStateException("target_changed_or_offline");
   JSONArray seen=new JSONArray(p.getString("seen","[]")),keepSeen=new JSONArray();
   for(int i=0;i<seen.length();i++){JSONObject item=seen.getJSONObject(i);if(now-item.optLong("at",0)<86400000){keepSeen.put(item);if(id.equals(item.optString("id")))return new JSONObject().put("ok",true).put("queued",true).put("duplicate",true);}}
   for(int i=0;i<queue.length();i++)if(id.equals(queue.getJSONObject(i).optString("requestId")))return new JSONObject().put("ok",true).put("queued",true).put("duplicate",true);
   if(queue.length()>=20)throw new IllegalStateException("queue_full");
   queue.put(new JSONObject().put("requestId",id).put("text",text).put("threadId",in.getString("threadId")).put("title",in.getString("title")).put("createdAt",now));
   if(keepSeen.length()>500)throw new IllegalStateException("receipt_limit");
   keepSeen.put(new JSONObject().put("id",id).put("at",now));
   if(!p.edit().putString("queue",queue.toString()).putString("seen",keepSeen.toString()).commit())throw new IllegalStateException("save_failed");
   return new JSONObject().put("ok",true).put("queued",true);
  }
  if(path.equals("pending"))return new JSONObject().put("requests",queue);
  if(path.equals("ack")){
   String id=new JSONObject(body).getString("requestId");JSONArray keep=new JSONArray();
   for(int i=0;i<queue.length();i++){JSONObject q=queue.getJSONObject(i);if(!id.equals(q.optString("requestId")))keep.put(q);}
   if(!p.edit().putString("queue",keep.toString()).commit())throw new IllegalStateException("save_failed");
   return new JSONObject().put("ok",true);
  }
  throw new IllegalArgumentException("unknown_codex_endpoint");
 }
}
