package com.example.rokidgeminisecretary;
import android.content.Context;import android.util.AtomicFile;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Last ten sent payloads, encrypted locally for 24 hours. Reading never enqueues. */
final class ConversationRecoveryStore {
 static final long TTL=86400000L;
 static File root(Context c){File d=new File(c.getFilesDir(),"conversation-recovery");d.mkdirs();return d;}
 static File file(Context c,String id)throws Exception{if(id==null||!id.matches("[a-f0-9]{64}"))throw new IOException("送信控えIDが不正です");return new File(root(c),id+".gcm");}
 static JSONObject safe(JSONObject input)throws Exception{
  String message=input.optString("message"),request=input.optString("requestId");if(message.length()>2800||!request.matches("[a-zA-Z0-9-]{8,80}"))throw new IOException("送信控えを保存できません");
  JSONObject out=new JSONObject().put("message",message).put("requestId",request);
  for(String key:new String[]{"referenceKey","parent"})if(input.has(key))out.put(key,input.optString(key));
  String image=input.optString("image");if(image.length()>800000)throw new IOException("添付が大きすぎます");
  if(!image.isEmpty()){out.put("image",image);int count=input.optInt("imageCount",1);out.put("imageCount",count).put("kinds",ImageAttachmentKinds.normalize(input.optJSONArray("kinds"),count));}return out;
 }
 static synchronized void save(Context c,String id,String route,JSONObject input)throws Exception{
  if(!route.matches("send|image-question|media-generate"))throw new IOException("送信控え種別が不正です");JSONObject payload=safe(input);File path=file(c,id);
  if(path.exists()){JSONObject old=read(c,id);if(!old.getString("route").equals(route)||!old.getJSONObject("payload").toString().equals(payload.toString()))throw new IOException("同じ送信IDの内容が変わりました。前の送信状況を確認してください");return;}
  prune(c);File[] current=root(c).listFiles((d,n)->n.endsWith(".gcm"));if(current!=null&&current.length>=10){Arrays.sort(current,Comparator.comparingLong(File::lastModified));new AtomicFile(current[0]).delete();}
  byte[] bytes=new JSONObject().put("id",id).put("at",System.currentTimeMillis()).put("route",route).put("payload",payload).toString().getBytes(StandardCharsets.UTF_8);AtomicFile f=new AtomicFile(path);FileOutputStream stream=null;try{stream=f.startWrite();stream.write(DeviceSecrets.photos().encrypt(bytes));f.finishWrite(stream);}catch(Exception e){if(stream!=null)f.failWrite(stream);throw e;}
 }
 static synchronized JSONObject read(Context c,String id)throws Exception{
  File path=file(c,id);JSONObject item=new JSONObject(new String(DeviceSecrets.photos().decrypt(new AtomicFile(path).readFully()),StandardCharsets.UTF_8));long age=System.currentTimeMillis()-item.getLong("at");if(age<0||age>=TTL){new AtomicFile(path).delete();throw new IOException("送信控えの期限が切れました");}return item;
 }
 static synchronized JSONArray list(Context c)throws Exception{prune(c);JSONArray rows=new JSONArray();File[] all=root(c).listFiles((d,n)->n.matches("[a-f0-9]{64}\\.gcm"));if(all==null)return rows;Arrays.sort(all,Comparator.comparingLong(File::lastModified).reversed());for(File f:all){try{JSONObject item=read(c,f.getName().substring(0,64)),p=item.getJSONObject("payload");rows.put(new JSONObject().put("id",item.getString("id")).put("at",item.getLong("at")).put("message",p.optString("message")).put("imageCount",p.has("image")?p.optInt("imageCount",1):0).put("route",item.getString("route")));}catch(Exception invalid){/* Keep corrupt data for diagnosis, never expose it. */}}return rows;}
 static void prune(Context c){File[] all=root(c).listFiles();if(all!=null)for(File f:all){long age=System.currentTimeMillis()-f.lastModified();if(age<0||age>=TTL)new AtomicFile(f).delete();}}
 private ConversationRecoveryStore(){}
}
