package com.example.rokidgeminisecretary;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Read-only foreign log ingestion. Assistant content is attributed reference data. */
public final class HirokidLogMonitor implements AutoCloseable {
 public static final String LOG_PATH="/sdcard/Android/data/com.rokid.sprite.global.aiapp/files/logs/app.log";
 private final Context context;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 private final Parser parser=new Parser();
 private long offset; private String boundary=""; private byte[] pending=new byte[0];
 private String lastStatus="";
 private static final Object JSON_LOCK=new Object();
 public HirokidLogMonitor(Context c){context=c.getApplicationContext();worker.scheduleWithFixedDelay(this::poll,0,3,TimeUnit.SECONDS);}
 public static void requestPermission(Activity a){
  if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()
    &&!a.getPreferences(0).getBoolean("hirokid_permission_requested",false)){
   a.getPreferences(0).edit().putBoolean("hirokid_permission_requested",true).apply();
   try{a.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,android.net.Uri.parse("package:"+a.getPackageName())));}
   catch(ActivityNotFoundException e){try{a.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}catch(Exception failure){Log.w("HirokidKnowledge","permission settings unavailable",failure);}}
  }
 }
 private void status(String value){if(!value.equals(lastStatus)){lastStatus=value;Log.i("HirokidKnowledge",value);context.getSharedPreferences("hirokid_monitor",0).edit().putString("status",value).apply();}}
 private void poll(){try{
  if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){status("permission_required");return;}
  try(RandomAccessFile file=new RandomAccessFile(LOG_PATH,"r")){
   long size=file.length();
   if(size<offset||!boundaryMatches(file)){offset=0;pending=new byte[0];parser.reset();}
   if(size==offset){status("monitoring");return;}
   if(size-offset>1024*1024){offset=Math.max(0,size-1024*1024);pending=new byte[0];parser.reset();file.seek(offset);if(offset>0)file.readLine();offset=file.getFilePointer();}
   file.seek(offset);byte[] bytes=new byte[(int)(size-offset)];file.readFully(bytes);offset=size;
   byte[] combined=new byte[pending.length+bytes.length];System.arraycopy(pending,0,combined,0,pending.length);System.arraycopy(bytes,0,combined,pending.length,bytes.length);
   int start=0;
   for(int i=0;i<combined.length;i++)if(combined[i]=='\n'){
    if(i-start<256*1024){JSONObject pair=null;try{pair=parser.accept(new String(combined,start,i-start,StandardCharsets.UTF_8));}catch(JSONException malformed){Log.w("HirokidKnowledge","malformed log message skipped");}if(pair!=null)sync(pair);}
    start=i+1;
   }
   pending=Arrays.copyOfRange(combined,start,combined.length);if(pending.length>256*1024)pending=new byte[0];
   file.seek(Math.max(0,offset-64));byte[] edge=new byte[(int)Math.min(64,offset)];file.readFully(edge);boundary=android.util.Base64.encodeToString(edge,android.util.Base64.NO_WRAP);
   status("monitoring");
  }
 }catch(Exception e){status("read_or_sync_failed:"+e.getClass().getSimpleName()+":"+e.getMessage());}}
 private boolean boundaryMatches(RandomAccessFile f)throws Exception{
  if(offset==0||boundary.isEmpty())return true;f.seek(Math.max(0,offset-64));byte[] b=new byte[(int)Math.min(64,offset)];f.readFully(b);return boundary.equals(android.util.Base64.encodeToString(b,android.util.Base64.NO_WRAP));
 }
 public static File knowledgeFile(){return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"Loki/shared_knowledge.json");}
 private void sync(JSONObject pair)throws Exception{
  synchronized(JSON_LOCK){File target=knowledgeFile();File folder=target.getParentFile();if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("knowledge directory unavailable");
   android.util.AtomicFile atomic=new android.util.AtomicFile(target);JSONObject root=new JSONObject();
   if(target.isFile()){try(InputStream in=atomic.openRead()){root=new JSONObject(read(in,256*1024));}}
   JSONObject previous=root.optJSONObject("hirokid_ai");if(previous!=null&&previous.toString().equals(pair.toString()))return;
   root.put("hirokid_ai",pair);FileOutputStream out=null;
   try{out=atomic.startWrite();out.write(root.toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(out);}catch(Exception e){if(out!=null)atomic.failWrite(out);throw e;}
   Log.i("HirokidKnowledge","pair_synced request="+pair.optString("request_id"));
  }
 }
 static String read(InputStream in,int max)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>max)throw new IOException("JSON limit exceeded");}return out.toString("UTF-8");}
 public static JSONObject state(Context c)throws Exception{
  JSONObject result=new JSONObject().put("permission",Build.VERSION.SDK_INT<30||Environment.isExternalStorageManager()).put("status",c.getSharedPreferences("hirokid_monitor",0).getString("status","not_started"));
  synchronized(JSON_LOCK){if(knowledgeFile().isFile())try(InputStream in=new android.util.AtomicFile(knowledgeFile()).openRead()){result.put("knowledge",new JSONObject(read(in,256*1024)));}}
  return result;
 }
 public static String contextForPrompt(Context c){try{return ExternalAiContext.format(state(c).optJSONObject("knowledge"),System.currentTimeMillis());}catch(Exception e){Log.w("HirokidKnowledge","context unavailable",e);return "";}}
 public static String contextForNavigation(Context c){return contextForPrompt(c);}
 public void close(){worker.shutdownNow();}
 public static final class Parser {
  private final LinkedHashMap<String,Turn> turns=new LinkedHashMap<>();
  private static final Pattern ID=Pattern.compile("request_id=([^,} ]+)");
  static final class Turn{String user="",answer="",stamp="";boolean userDone,answerDone;}
  public void reset(){turns.clear();}
  public JSONObject accept(String line)throws Exception{
   if(!line.contains("WSMessage(")||!line.contains("scene='ai_assist'")||!line.contains("sender='SERVER'"))return null;
   Matcher m=ID.matcher(line);if(!m.find())return null;String id=m.group(1);Turn t=turns.get(id);if(t==null){t=new Turn();turns.put(id,t);if(turns.size()>32)turns.remove(turns.keySet().iterator().next());}
   if(line.contains("type='recognized_speech'")){int at=line.indexOf(", text=");int end=line.lastIndexOf("}, audioSize=");if(at>=0&&end>at)t.user=line.substring(at+7,end).trim();}
   else if(line.contains("type='sentence_complete'"))t.userDone=true;
   else if(line.contains("type='llm'")){
    int at=line.indexOf("message={");if(at>=0){String json=objectAt(line,at+8);JSONObject message=new JSONObject(json);if("text".equals(message.optString("type"))){String content=message.optString("content");if(t.answer.length()+content.length()<=12000)t.answer+=content;}}
    if(line.contains("is_final=true"))t.answerDone=true;
   }else return null;
   t.stamp=line.length()>=23?line.substring(0,23):"";
   if(t.userDone&&t.answerDone&&!t.user.isEmpty()&&!t.answer.isEmpty()){
    JSONObject pair=new JSONObject().put("last_user_query",t.user).put("last_ai_response",t.answer).put("timestamp",t.stamp).put("request_id",id).put("source","Hi Rokid app.log");turns.remove(id);return pair;
   }return null;
  }
  private static String objectAt(String s,int at)throws Exception{int depth=0;boolean quoted=false,escape=false;for(int i=at;i<s.length();i++){char c=s.charAt(i);if(quoted){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quoted=false;}else{if(c=='"')quoted=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0)return s.substring(at,i+1);}}throw new JSONException("incomplete message JSON");}
 }
}