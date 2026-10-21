package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;
/** Receipt ownership and queue mutations share the same lock order. Never edits a received turn. */
final class QueuedConversation {
 static JSONObject change(Context c,JSONObject input)throws Exception{
  synchronized(LokiWebConsole.class){synchronized(MainActivity.class){
   String key=input.getString("key"),action=input.getString("action"),change=input.getString("changeId");
   if(!TrackedWebRequest.validId(key)||!action.matches("cancel|edit")||!change.matches("[a-zA-Z0-9-]{8,80}"))throw new IOException("操作を選び直してください");
   SharedPreferences p=c.getSharedPreferences("loki_web_console",0);JSONArray all=new JSONArray(p.getString("outbox","[]"));JSONObject old=null;
   for(int i=0;i<all.length();i++)if(key.equals(all.getJSONObject(i).optString("key"))){old=all.getJSONObject(i);break;}
   if(old==null||!old.optBoolean("tracked"))throw new IOException("送信記録がありません");
   String newKey=RecorderPhotoStore.hash((key+":"+change).getBytes(StandardCharsets.UTF_8));
   if(action.equals("cancel")&&old.optString("requestState").equals("CANCELLED"))return new JSONObject().put("cancelled",true).put("duplicate",true);
   if(action.equals("edit")&&old.optString("requestState").equals("SUPERSEDED")&&newKey.equals(old.optString("replacementKey"))){for(int i=0;i<all.length();i++)if(newKey.equals(all.getJSONObject(i).optString("key"))){JSONObject row=all.getJSONObject(i);if(!input.getString("message").trim().equals(row.optString("message")))throw new IOException("同じ操作IDの内容が変わりました");return new JSONObject().put("edited",true).put("duplicate",true).put("requestKey",newKey).put("tracked",true).put("queued",row.optBoolean("accepted"));}}
   if(!old.optString("requestState").equals("QUEUED")||!MainActivity.hasQueuedWebCommand(c,key))throw new IOException("受信済み・処理開始後は編集や取消ができません。最新の状態を確認してください");
   JSONObject next=null;String wire=null;
   if(action.equals("edit")){String text=input.getString("message").trim(),reference=old.optString("referenceKey");wire=TrackedWebRequest.wire(newKey,text,reference);
    ConversationRecoveryStore.save(c,newKey,"send",new JSONObject().put("requestId",change).put("message",text).put("referenceKey",reference));
    next=new JSONObject().put("key",newKey).put("time",System.currentTimeMillis()).put("message",text).put("referenceKey",reference).put("device",old.optString("device")).put("status","編集を受付中").put("tracked",true).put("requestState","CREATED");}
   old.put("requestState",action.equals("cancel")?"CANCELLED":"SUPERSEDED").put("receiptAt",System.currentTimeMillis()).put("status",action.equals("cancel")?"送信前に取消済み":"編集した依頼へ差し替え済み");if(next!=null)old.put("replacementKey",newKey);
   JSONArray updated=next==null?all:WebRequestLedger.append(all,next);if(updated==null)throw new IOException("送信記録が満杯です");
   if(!p.edit().putString("outbox",updated.toString()).commit())throw new IOException("取消状態を保存できません");
   // A crash here leaves the old ID terminal; receipt handling cannot grant execution.
   boolean changed=MainActivity.replaceQueuedWebCommand(c,key,wire);
   if(next!=null){next.put("accepted",changed).put("requestState",changed?"QUEUED":"FAILED").put("status",changed?"編集後の送信待ち":"編集後の送信は未確認");if(!p.edit().putString("outbox",updated.toString()).commit())throw new IOException("編集結果を確認してください。自動再送はしていません");}
   if(!changed)throw new IOException("古い依頼は取り消しましたが、キュー更新を確認できません。送信状況を確認してください");
   return new JSONObject().put("cancelled",next==null).put("edited",next!=null).put("queued",next!=null).put("tracked",next!=null).put("requestKey",next==null?key:newKey);
  }}
 }
 private QueuedConversation(){}
}
