package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;import java.util.*;import java.nio.file.*;
public final class V735PhoneChecks {
 static int n;static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 public static int run(Context c)throws Exception{
  n=0;String old="a".repeat(64),fresh="b".repeat(64);long now=System.currentTimeMillis();
  c.getSharedPreferences("loki_web_console",0).edit().putString("outbox",new JSONArray().put(new JSONObject().put("key",old).put("time",now-180000)).put(new JSONObject().put("key",fresh).put("time",now)).toString()).commit();
  ReconnectReview.prefs(c).edit().clear().commit();ReconnectReview.last=-1;ReconnectReview.ever=false;
  ArrayList<String> queue=new ArrayList<>();queue.add(TrackedWebRequest.wire(old,"old synthetic"));queue.add(TrackedWebRequest.wire(fresh,"fresh synthetic"));MainActivity.persistReviewCommands(c,queue);
  JSONArray order=new JSONArray().put(new JSONObject().put("id",old)).put(new JSONObject().put("id",fresh));ReconnectReview.contact(c,100,order);
  ok(ReconnectReview.held(c,old),"old request stays held");ok(!ReconnectReview.held(c,fresh),"fresh request is not incorrectly held");ok(ReconnectReview.dispatchIndex(c,queue)==1,"fresh request bypasses old held request");ok(MainActivity.pendingReviewCommands(c).size()==2,"reconnect does not cancel or duplicate");
  ReconnectReview.approve(c,old);ok(!ReconnectReview.held(c,old),"one selected request released");ok(MainActivity.pendingReviewCommands(c).size()==2,"approval keeps original request identity");
  String id=UUID.randomUUID().toString(),wire=QueuedImageReview.PREFIX+new JSONObject().put("id",id);queue.clear();queue.add(TrackedWebRequest.wire(old,"retained"));queue.add(wire);MainActivity.persistReviewCommands(c,queue);
  JSONObject packet=new JSONObject().put("id",id).put("question","合成添付の質問").put("expires",now-100).put("data","/9j/2Q==");ImageQuestions.save(ImageQuestions.file(c,id),packet);
  byte[] before=Files.readAllBytes(ImageQuestions.file(c,id).toPath());ImageQuestions.prune(c);ok(ImageQuestions.file(c,id).isFile(),"pending expired image retained by pruning");JSONArray rows=QueuedImageReview.rows(c);ok(rows.length()==1&&rows.getJSONObject(0).optBoolean("available"),"expired attachment is available for explicit review");ok(Arrays.equals(before,Files.readAllBytes(ImageQuestions.file(c,id).toPath())),"listing never refreshes or sends attachment");
  QueuedImageReview.resume(c,QueuedImageReview.key(wire));ArrayList<String> resumed=MainActivity.pendingReviewCommands(c);ok(resumed.size()==2&&resumed.get(0).equals(wire)&&resumed.get(1).contains(old),"explicit selected image only promoted, no loss");ok(ImageQuestions.read(c,id).getLong("expires")>now,"only explicit resume renews delivery window");
  boolean refused=false;try{QueuedImageReview.resume(c,"c".repeat(64));}catch(Exception expected){refused=true;}ok(refused&&MainActivity.pendingReviewCommands(c).size()==2,"stale selection cannot execute another queued item");
  String missing=UUID.randomUUID().toString();queue.add(QueuedImageReview.PREFIX+new JSONObject().put("id",missing));MainActivity.persistReviewCommands(c,queue);rows=QueuedImageReview.rows(c);ok(!rows.getJSONObject(1).optBoolean("available"),"missing attachment reported without deleting request");
  return n;
 }
}
