package com.example.rokidgeminisecretary;
import android.content.*;import android.app.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.util.*;import org.json.*;
/** An old attachment is never reactivated until the user selects that exact queue item. */
final class QueuedImageReview {
 static final String PREFIX="__LOKI_IMAGE__:";
 static String cachedSignature="",cachedRows="[]";
 static String key(String wire)throws Exception{return RecorderPhotoStore.hash(wire.getBytes(StandardCharsets.UTF_8));}
 static JSONObject packet(Context c,String wire)throws Exception{
  String id=new JSONObject(wire.substring(PREFIX.length())).getString("id");File file=ImageQuestions.file(c,id);
  if(file.length()>2000000L)throw new IOException("添付の保存サイズを確認できません");
  return new JSONObject(new String(DeviceSecrets.photos().decrypt(Files.readAllBytes(file.toPath())),StandardCharsets.UTF_8));
 }
 static JSONArray rows(Context c)throws Exception{synchronized(MainActivity.class){ArrayList<String> queue=MainActivity.pendingReviewCommands(c);StringBuilder signature=new StringBuilder().append(System.currentTimeMillis()/60000L);for(String wire:queue)if(wire.startsWith(PREFIX)){signature.append(key(wire));try{File file=ImageQuestions.file(c,new JSONObject(wire.substring(PREFIX.length())).getString("id"));signature.append(':').append(file.lastModified()).append(':').append(file.length());}catch(Exception missing){signature.append(":missing");}}if(signature.toString().equals(cachedSignature))return new JSONArray(cachedRows);JSONArray out=new JSONArray();for(String wire:queue)if(wire.startsWith(PREFIX)){
  JSONObject row=new JSONObject().put("key",key(wire)).put("available",false).put("message","画像付きの質問：添付を確認してください");
  try{JSONObject p=packet(c,wire);row.put("message",p.optString("question")).put("available",!p.optBoolean("complete")&&!p.optString("data").isEmpty()).put("expired",p.optLong("expires")<System.currentTimeMillis());}catch(Exception missing){}
  out.put(row);
 }cachedSignature=signature.toString();cachedRows=out.toString();return out;}}
 static JSONObject resume(Context c,String key)throws Exception{synchronized(MainActivity.class){
  if(key==null||!key.matches("[a-f0-9]{64}"))throw new IOException("画像の選択が不正です");
  ArrayList<String> queue=MainActivity.pendingReviewCommands(c);String selected=null;for(String wire:queue)if(wire.startsWith(PREFIX)&&key(wire).equals(key)){selected=wire;break;}
  if(selected==null)throw new IOException("受信済み、または送信状況が変わっています。更新してください");
  JSONObject p=packet(c,selected);if(p.optBoolean("complete")||p.optString("data").isEmpty())throw new IOException("添付原本がありません。送信控えか画像から選び直してください");
  p.put("expires",System.currentTimeMillis()+ImageQuestions.TTL);ImageQuestions.save(ImageQuestions.file(c,p.getString("id")),p);
  queue.remove(selected);queue.add(0,selected);if(!MainActivity.persistReviewCommands(c,queue))throw new IOException("再送の受付を保存できません。送信状況を確認してください");
  return new JSONObject().put("ok",true);
 }}
 static void show(MainActivity c){try{JSONArray rows=rows(c);String[] labels=new String[rows.length()];for(int i=0;i<labels.length;i++){JSONObject row=rows.getJSONObject(i);labels[i]=(row.optBoolean("available")?"再送を確認：":"添付の再選択が必要：")+row.optString("message");}
  new AlertDialog.Builder(c).setTitle("画像付きの送信待ち（自動再送しません）").setItems(labels,(d,i)->{JSONObject row=rows.optJSONObject(i);new AlertDialog.Builder(c).setMessage(row.optString("message")+"\n添付が残っている場合だけ、この1件の送信を再開します。API利用が発生する場合があります。").setPositiveButton("この1件を再送",(a,b)->{try{resume(c,row.optString("key"));android.widget.Toast.makeText(c,"選択した画像の送信待ちを再開しました",1).show();}catch(Exception e){new AlertDialog.Builder(c).setMessage(e.getMessage()).setPositiveButton("閉じる",null).show();}}).setNegativeButton("戻る",null).show();}).setPositiveButton("閉じる",null).show();
 }catch(Exception e){new AlertDialog.Builder(c).setMessage("画像の送信待ちを確認できません。元の依頼は保持しています。").setPositiveButton("閉じる",null).show();}}
 private QueuedImageReview(){}
}
