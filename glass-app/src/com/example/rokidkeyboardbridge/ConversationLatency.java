package com.example.rokidkeyboardbridge;
import org.json.*;
/** Monotonic offsets on glasses only. No prompt, answer, address or credentials. RAM only. */
public final class ConversationLatency {
 private static String id="";private static long begin=-1;private static JSONObject stages=new JSONObject();
 public static synchronized void begin(String key,long now){if(key==null||!key.matches("[a-f0-9]{64}")||now<0||key.equals(id))return;id=key;begin=now;stages=new JSONObject();}
 public static synchronized void mark(String key,String stage,long now){if(!id.equals(key)||begin<0||now<begin||now-begin>600000||!stage.matches("modelStartMs|firstTextMs|speechDispatchMs")||stages.has(stage))return;try{stages.put(stage,now-begin);}catch(Exception ignored){}}
 public static synchronized JSONObject snapshot(){try{return new JSONObject(stages.toString()).put("measuredAt",System.currentTimeMillis()).put("speechAudibleStart","unavailable").put("scope","glasses monotonic since received; speech dispatch is not audible start");}catch(Exception e){return new JSONObject();}}
 private ConversationLatency(){}
}
