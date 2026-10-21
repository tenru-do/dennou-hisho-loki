package com.example.rokidgeminisecretary;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Phone is authoritative. Encrypted, bounded, atomic storage. Never auto-executes pending tasks. */
final class SecretaryInbox {
 private static final int MAX=100;
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"secretary-inbox-v1.gcm"));}
 private static JSONArray read(Context c)throws Exception {
  AtomicFile f=file(c);if(!f.getBaseFile().exists()&&!new File(f.getBaseFile()+".bak").exists())return new JSONArray();
  byte[] b=f.readFully();if(b.length>4000000)throw new IOException("受信箱が大きすぎます。元データは保持しています。");
  return new JSONArray(new String(DeviceSecrets.photos().decrypt(b),StandardCharsets.UTF_8));
 }
 private static void write(Context c,JSONArray all)throws Exception {
  byte[] plain=all.toString().getBytes(StandardCharsets.UTF_8);if(plain.length>3900000)throw new IOException("受信箱の保存上限です。元の記録は保持しています");byte[] b=DeviceSecrets.photos().encrypt(plain);
  AtomicFile f=file(c);FileOutputStream out=null;
  try{out=f.startWrite();out.write(b);f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
 }
 private static JSONObject find(JSONArray a,String id)throws Exception {for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).optString("id"))&&a.getJSONObject(i).optLong("deletedAt")==0)return a.getJSONObject(i);throw new IOException("対象の案件はありません。受信箱を開き直してください。");}
 static String display(JSONObject card){
  StringBuilder out=new StringBuilder("保留した依頼：").append(card.optString("request")).append("\n状態：").append(card.optString("stateLabel",card.optString("state"))).append("\n\n保存時点の会話：\n");
  for(String key:new String[]{"subject","date_text","conditions"})if(!card.optString(key).isEmpty())out.append(key.equals("subject")?"対象：":key.equals("date_text")?"日程：":"条件：").append(card.optString(key)).append("\n");
  try{JSONArray rows=new JSONObject(card.optString("reference")).optJSONArray("conversation");if(rows!=null)for(int i=rows.length()-1;i>=0;i--){JSONObject row=rows.optJSONObject(i);if(row!=null)out.append(row.optString("kind")).append("：").append(row.optString("message")).append("\n\n");}}catch(Exception e){out.append(card.optString("reference"));}
  if(card.optJSONArray("updates")!=null)out.append("\n変更内容：").append(card.optJSONArray("updates").toString()).append("\n");
  if("needs_check".equals(card.optString("displayStage")))out.append("\n登録結果を確認できません。カレンダーを確認してください。自動で再登録しません。\n");
  return out.append("\n日程・出典は保存時点の情報です。登録の前に日時と登録先を確認します。").toString();
 }
 static synchronized JSONObject list(Context c)throws Exception {
  JSONArray all=read(c),items=new JSONArray();
  for(int i=all.length()-1;i>=0;i--){if(all.getJSONObject(i).optLong("deletedAt")>0)continue;JSONObject j=SecretaryInboxPresentation.decorate(all.getJSONObject(i),System.currentTimeMillis());j.remove("reference");j.remove("review");j.remove("reviewToken");j.remove("completedToken");j.remove("key");items.put(j);}
  return new JSONObject().put("ok",true).put("items",items);
 }
 static synchronized JSONObject get(Context c,String id)throws Exception {JSONObject j=SecretaryInboxPresentation.decorate(find(read(c),id),System.currentTimeMillis());j.remove("review");j.remove("reviewToken");j.remove("completedToken");j.remove("key");return new JSONObject().put("ok",true).put("item",j);}
 static synchronized JSONObject save(Context c,String request,String reference,String requestId)throws Exception {return save(c,request,reference,requestId,null);}
 static synchronized JSONObject save(Context c,String request,String reference,String requestId,JSONObject metadata)throws Exception {
  request=request.trim();if(request.isEmpty()||request.length()>2400||reference.length()>28000)throw new IOException("案件の内容が長すぎます。対象を分けて保存してください。");
  if(!requestId.matches("[a-f0-9-]{36}"))throw new IOException("保存要求IDが無効です。");
  String key=DeviceSecrets.auditHash(requestId);
  JSONArray all=read(c);
  for(int i=0;i<all.length();i++){JSONObject j=all.getJSONObject(i);if(key.equals(j.optString("key")))return new JSONObject().put("ok",true).put("item",j).put("duplicate",true);}
  if(list(c).getJSONArray("items").length()>=MAX||all.length()>=200)throw new IOException("受信箱は100件までです。不要な案件を削除してください。");
  JSONArray urls=new JSONArray();java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://[^\\s<>\"\\\\]+").matcher(reference);
  while(m.find()&&urls.length()<12)urls.put(m.group());
  JSONObject j=new JSONObject().put("id",UUID.randomUUID().toString()).put("key",key).put("title",request.length()>160?request.substring(0,160):request)
   .put("request",request).put("reference",reference).put("sources",urls).put("createdAt",System.currentTimeMillis()).put("state","pending").put("revision",1);
  if(metadata!=null)metadata(j,metadata);
  all.put(j);write(c,all);return new JSONObject().put("ok",true).put("item",j);
 }
 private static void metadata(JSONObject j,JSONObject m)throws Exception {
  for(String key:new String[]{"subject","date_text","conditions"}){
   String value=m.optString(key).trim();if(value.length()>(key.equals("subject")?160:800))throw new IOException("案件の項目が長すぎます。");
   if(!value.isEmpty()){j.put(key,value);if(key.equals("subject"))j.put("title",value);}
  }
 }
 static synchronized JSONObject action(Context c,JSONObject input)throws Exception {
  JSONArray all=read(c);JSONObject j=null;for(int i=0;i<all.length();i++)if(input.getString("id").equals(all.getJSONObject(i).getString("id")))j=all.getJSONObject(i);if(j==null)throw new IOException("案件がありません");
  if(j.optLong("deletedAt")>0&&!input.getString("action").matches("restore|purge"))throw new IOException("削除済みです。ごみ箱から復元してください");
  if(j.optInt("revision")!=input.getInt("revision"))throw new IOException("案件が更新されています。受信箱を開き直してください。");
  String op=input.getString("action");
  if("delete".equals(op)){ReviewStore.history(j);j.put("deletedAt",System.currentTimeMillis()).put("revision",j.getInt("revision")+1);j.remove("review");j.remove("reviewToken");write(c,all);}
  else if("restore".equals(op)){if(j.optLong("deletedAt")==0||list(c).getJSONArray("items").length()>=100)throw new IOException("復元対象・件数を確認してください");j.remove("deletedAt");if(j.optString("state").equals("executing"))j.put("state","unknown");j.put("revision",j.getInt("revision")+1);write(c,all);}
  else if("purge".equals(op)){if(j.optLong("deletedAt")==0)throw new IOException("ごみ箱の案件だけ完全削除できます");JSONArray next=new JSONArray();for(int i=0;i<all.length();i++)if(!input.getString("id").equals(all.getJSONObject(i).getString("id")))next.put(all.get(i));write(c,next);}
  else if("complete_manual".equals(op)){if(!"pending".equals(j.optString("state")))throw new IOException("保留中の案件だけを手動完了できます");j.put("state","cancelled").put("manualCompleted",true).put("revision",j.getInt("revision")+1);j.remove("review");j.remove("reviewToken");write(c,all);}
  else if("cancel".equals(op)){if(!"pending".equals(j.optString("state")))throw new IOException("実行中・結果未確認・完了の案件は保留へ戻せません。");j.put("state","cancelled").put("revision",j.getInt("revision")+1);j.remove("review");j.remove("reviewToken");write(c,all);}
  else if("update".equals(op)){
   if(!"pending".equals(j.optString("state")))throw new IOException("保留中の案件だけを変更できます。");
   ReviewStore.history(j);String change=input.getString("change").trim();if(change.isEmpty()||change.length()>1000)throw new IOException("変更内容を確認してください。");
   JSONArray updates=j.optJSONArray("updates");if(updates==null)updates=new JSONArray();if(updates.length()>=10)throw new IOException("この案件の変更は10回までです。内容を確認して整理してください。");
   JSONObject update=new JSONObject().put("at",System.currentTimeMillis()).put("request",change);
   if(input.optJSONObject("metadata")!=null){metadata(j,input.getJSONObject("metadata"));update.put("details",input.getJSONObject("metadata"));}
   updates.put(update);j.put("updates",updates).put("revision",j.getInt("revision")+1).put("updatedAt",System.currentTimeMillis());j.remove("review");j.remove("reviewToken");write(c,all);
  }
  else if("edit-fields".equals(op)||"undo-edit".equals(op)){if(!"pending".equals(j.optString("state")))throw new IOException("保留中の案件の本文だけ編集できます");JSONObject value=input;if(op.equals("undo-edit")){JSONArray history=j.optJSONArray("history");int index=input.getInt("index");if(history==null||index<0||index>=history.length())throw new IOException("履歴がありません");value=history.getJSONObject(index);}String request=value.getString("request").trim(),title=value.getString("title").trim();if(request.isEmpty()||request.length()>2400||title.isEmpty()||title.length()>160)throw new IOException("案件の本文・件名を確認してください");ReviewStore.history(j);j.put("request",request).put("title",title);for(String field:new String[]{"conditions","date_text"})if(value.has(field))j.put(field,value.getString(field));j.put("revision",j.getInt("revision")+1).put("updatedAt",System.currentTimeMillis());j.remove("review");j.remove("reviewToken");write(c,all);}
  else throw new IOException("この操作は使えません。");
  return new JSONObject().put("ok",true);
 }
 static synchronized JSONArray trash(Context c)throws Exception{JSONArray all=read(c),out=new JSONArray();for(int i=0;i<all.length();i++){JSONObject row=all.getJSONObject(i);if(row.optLong("deletedAt")>0)out.put(new JSONObject().put("id",row.getString("id")).put("revision",row.getInt("revision")).put("title",row.optString("title")).put("request",row.optString("request")).put("deletedAt",row.optLong("deletedAt")));}return out;}
 static synchronized JSONObject review(Context c,JSONObject input)throws Exception {
  JSONArray all=read(c);JSONObject j=find(all,input.getString("id"));
  if(!"pending".equals(j.optString("state"))||j.getInt("revision")!=input.getInt("revision"))throw new IOException("この案件は更新済みか、処理済みです。");
  JSONObject candidate=input.getJSONObject("candidate");String name=candidate.getString("calendar_name");int matches=0;
  if(!CalendarWriter.permitted(c))throw new IOException("スマホのカレンダー権限を許可してください。");
  JSONArray calendars=CalendarWriter.calendars(c);for(int i=0;i<calendars.length();i++)if(name.equals(calendars.getJSONObject(i).getString("name")))matches++;
  if(matches!=1)throw new IOException("登録先のカレンダー名を確認してください。");
  JSONArray events=candidate.getJSONArray("events");if(events.length()<1||events.length()>10)throw new IOException("予定は1〜10件で確認してください。");
  for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);if(e.getString("title").trim().isEmpty()||e.getString("title").length()>200)throw new IOException("予定名を確認してください。");
   long start,end;if(e.getBoolean("all_day")){start=java.time.LocalDate.parse(e.getString("start")).toEpochDay()*86400000L;end=java.time.LocalDate.parse(e.getString("end")).toEpochDay()*86400000L;}
   else {start=java.time.OffsetDateTime.parse(e.getString("start")).toInstant().toEpochMilli();end=java.time.OffsetDateTime.parse(e.getString("end")).toInstant().toEpochMilli();}
   if(end<=start||end-start>366L*86400000)throw new IOException("開始・終了日時を確認してください。");
  }
  String token=UUID.randomUUID().toString();j.put("review",candidate).put("reviewToken",token).put("reviewAt",System.currentTimeMillis());write(c,all);
  return new JSONObject().put("ok",true).put("token",token);
 }
 static synchronized JSONObject confirm(Context c,JSONObject input)throws Exception {
  JSONArray all=read(c);JSONObject j=find(all,input.getString("id"));
  if("completed".equals(j.optString("state"))&&input.getString("token").equals(j.optString("completedToken")))return j.getJSONObject("result");
  if(!"pending".equals(j.optString("state"))||!input.getString("token").equals(j.optString("reviewToken"))||System.currentTimeMillis()-j.optLong("reviewAt")<0||System.currentTimeMillis()-j.optLong("reviewAt")>600000)throw new IOException("登録確認が失効しているか、結果未確認です。再送前にカレンダーと案件を確認してください。");
  JSONObject candidate=j.getJSONObject("review");j.put("state","executing");write(c,all);
  JSONObject result;
  try{result=CalendarWriter.save(c,candidate);}catch(Exception e){j.put("state","unknown");write(c,all);throw new IOException("登録結果を確認できません。カレンダーを確認してください。");}
  j.put("state",result.optBoolean("ok")?"completed":"pending").put("result",result).put("completedAt",System.currentTimeMillis()).put("completedToken",input.getString("token")).put("revision",j.getInt("revision")+1);
  j.remove("review");j.remove("reviewToken");write(c,all);return result;
 }
}
