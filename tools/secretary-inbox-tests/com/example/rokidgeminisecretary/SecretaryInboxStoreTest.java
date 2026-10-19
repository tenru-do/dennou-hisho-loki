package com.example.rokidgeminisecretary;
import org.json.*;import android.content.Context;import java.nio.file.*;import java.util.*;
public class SecretaryInboxStoreTest{
 static int checks;
 static void ok(boolean b,String name){if(!b)throw new AssertionError(name);checks++;}
 interface Throwing{void run()throws Exception;}
 static void reject(Throwing f,String name)throws Exception{try{f.run();}catch(Exception e){checks++;return;}throw new AssertionError(name);}
 static JSONObject save(Context c,String name)throws Exception{return SecretaryInbox.save(c,name,"{\"conversation\":[{\"kind\":\"Gemini\",\"message\":\"秋の市場 2026-10-24 https://example.org/event\"}]} ",UUID.randomUUID().toString()).getJSONObject("item");}
 static JSONObject candidate(){return new JSONObject().put("calendar_name","家族").put("events",new JSONArray().put(new JSONObject().put("title","秋の市場").put("start","2026-10-24").put("end","2026-10-25").put("all_day",true)));}
 static JSONObject review(Context c,JSONObject j)throws Exception{return SecretaryInbox.review(c,new JSONObject().put("id",j.getString("id")).put("revision",j.getInt("revision")).put("candidate",candidate()));}
 static JSONObject confirm(JSONObject item,String token){return new JSONObject().put("id",item.getString("id")).put("token",token);}
 public static void main(String[]args)throws Exception{
  Context c=new Context(Files.createTempDirectory("loki-inbox-host-").toFile());
  ok(SecretaryInbox.list(c).getJSONArray("items").length()==0,"empty");
  JSONObject item=save(c,"このイベントは家族に聞いてから登録したい");ok(CalendarWriter.writes==0,"save does not execute");
  ok(item.getJSONArray("sources").length()==1,"source retained");ok(SecretaryInbox.get(c,item.getString("id")).getJSONObject("item").getString("reference").contains("2026-10-24"),"date retained");
  String disk=Files.readString(c.getFilesDir().toPath().resolve("secretary-inbox-v1.gcm"),java.nio.charset.StandardCharsets.ISO_8859_1);ok(!disk.contains("example.org")&&!disk.contains("2026-10-24"),"encrypted disk");
  ok(SecretaryInbox.list(new Context(c.getFilesDir())).getJSONArray("items").length()==1,"restart");
  String requestId=UUID.randomUUID().toString();SecretaryInbox.save(c,"同じ依頼","元の日程",requestId);SecretaryInbox.save(c,"同じ依頼","変わった参照",requestId);ok(SecretaryInbox.list(c).getJSONArray("items").length()==2,"retry dedup");
  reject(()->SecretaryInbox.save(c,"bad","", "../bad"),"bad request id");
  JSONObject stale=new JSONObject().put("id",item.getString("id")).put("revision",99).put("candidate",candidate());reject(()->SecretaryInbox.review(c,stale),"revision conflict");
  JSONObject bad=new JSONObject().put("id",item.getString("id")).put("revision",1).put("candidate",candidate().put("calendar_name","別のカレンダー"));reject(()->SecretaryInbox.review(c,bad),"calendar mismatch");
  JSONObject badDate=candidate();badDate.getJSONArray("events").getJSONObject(0).put("end","2026-10-23");reject(()->SecretaryInbox.review(c,new JSONObject().put("id",item.getString("id")).put("revision",1).put("candidate",badDate)),"invalid date");
  JSONObject firstReview=review(c,item);ok(CalendarWriter.writes==0,"review does not execute");
  Path displayDisk=c.getFilesDir().toPath().resolve("secretary-inbox-v1.gcm");byte[] beforeDisplay=Files.readAllBytes(displayDisk);JSONObject shown=SecretaryInbox.get(c,item.getString("id")).getJSONObject("item");ok(shown.getString("displayStage").equals("review_wait")&&!shown.has("reviewToken")&&!shown.has("review"),"read-only review projection hides authorization");SecretaryInbox.list(c);ok(Arrays.equals(beforeDisplay,Files.readAllBytes(displayDisk))&&CalendarWriter.writes==0,"display does not rewrite storage or register");
  reject(()->SecretaryInbox.confirm(c,confirm(item,"wrong")),"wrong token");
  JSONObject newerReview=review(c,item);reject(()->SecretaryInbox.confirm(c,confirm(item,firstReview.getString("token"))),"superseded review");
  String token=newerReview.getString("token");ok(SecretaryInbox.confirm(c,confirm(item,token)).optBoolean("ok"),"confirmed save");
  SecretaryInbox.confirm(c,confirm(item,token));ok(CalendarWriter.writes==1,"retry returns receipt no second write");
  ok("completed".equals(SecretaryInbox.get(c,item.getString("id")).getJSONObject("item").getString("state")),"completed persisted");
  ok(!SecretaryInbox.list(c).getJSONArray("items").getJSONObject(0).has("reviewToken"),"token hidden");
  JSONObject interrupted=save(c,"別の予定を保留して");String t=review(c,interrupted).getString("token");CalendarWriter.fail=true;reject(()->SecretaryInbox.confirm(c,confirm(interrupted,t)),"interrupted registration");CalendarWriter.fail=false;
  int writes=CalendarWriter.writes;reject(()->SecretaryInbox.confirm(c,confirm(interrupted,t)),"unknown result not resent");ok(CalendarWriter.writes==writes,"unknown no repeat");
  JSONObject cancel=save(c,"保留の取消");JSONObject r=review(c,cancel);SecretaryInbox.action(c,new JSONObject().put("id",cancel.getString("id")).put("revision",1).put("action","cancel"));
  reject(()->SecretaryInbox.confirm(c,confirm(cancel,r.getString("token"))),"cancel invalidates review");
  reject(()->SecretaryInbox.action(c,new JSONObject().put("id",cancel.getString("id")).put("revision",1).put("action","delete")),"stale delete conflict");
  SecretaryInbox.action(c,new JSONObject().put("id",cancel.getString("id")).put("revision",2).put("action","delete"));reject(()->SecretaryInbox.get(c,cancel.getString("id")),"deleted absent");
  JSONObject expired=save(c,"期限試験");String et=review(c,expired).getString("token");
  Path path=c.getFilesDir().toPath().resolve("secretary-inbox-v1.gcm");JSONArray data=new JSONArray(new String(DeviceSecrets.photos().decrypt(Files.readAllBytes(path)),java.nio.charset.StandardCharsets.UTF_8));for(int i=0;i<data.length();i++)if(expired.getString("id").equals(data.getJSONObject(i).optString("id")))data.getJSONObject(i).put("reviewAt",System.currentTimeMillis()-600001);Files.write(path,DeviceSecrets.photos().encrypt(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  reject(()->SecretaryInbox.confirm(c,confirm(expired,et)),"expired review");
  JSONObject updated=save(c,"相談して決める");String oldToken=review(c,updated).getString("token");
  SecretaryInbox.action(c,new JSONObject().put("id",updated.getString("id")).put("revision",1).put("action","update").put("change","日程が11月3日に変わった").put("metadata",new JSONObject().put("date_text","11月3日").put("conditions","家族に確認済み")));
  JSONObject changed=SecretaryInbox.get(c,updated.getString("id")).getJSONObject("item");
  ok(changed.getInt("revision")==2&&changed.getString("date_text").equals("11月3日"),"confirmed date change persists");
  ok(changed.getJSONArray("updates").length()==1,"original reference retained with update trail");
  reject(()->SecretaryInbox.confirm(c,confirm(updated,oldToken)),"old calendar review invalidated on update");
  reject(()->SecretaryInbox.action(c,new JSONObject().put("id",updated.getString("id")).put("revision",1).put("action","update").put("change","stale")),"stale update rejected");
  JSONObject named=SecretaryInbox.save(c,"家族と相談して決めようかな","秋の市場は10月24日",UUID.randomUUID().toString(),new JSONObject().put("subject","秋の市場").put("date_text","10月24日").put("conditions","家族と相談")).getJSONObject("item");
  ok(named.getString("title").equals("秋の市場")&&named.getString("conditions").equals("家族と相談"),"confirmed semantic metadata retained");
  byte[] corrupted=Files.readAllBytes(path);corrupted[corrupted.length-1]^=1;Files.write(path,corrupted);reject(()->SecretaryInbox.list(c),"tamper rejected");ok(Arrays.equals(corrupted,Files.readAllBytes(path)),"corruption not overwritten");
  System.out.println("PASS secretary inbox store "+checks+" checks (host Android/calendar doubles; production vault cipher)");
 }
}