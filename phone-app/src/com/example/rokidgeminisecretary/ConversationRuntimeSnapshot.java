package com.example.rokidgeminisecretary;
import org.json.*;
/** Read-only, content-free state freshness uses phone monotonic time, not glasses clock. */
final class ConversationRuntimeSnapshot {
 static long age(long now,long at){return at>0&&now>=at?now-at:-1;}
 static JSONObject build(String state,String message,long now,long contact,long received,long waitMs,int queued)throws Exception{
  long contactAge=age(now,contact),stateAge=age(now,received);
  return new JSONObject().put("state",state==null?"UNKNOWN":state).put("message",message==null?"":message.substring(0,Math.min(80,message.length())))
   .put("contactRecent",contactAge>=0&&contactAge<=15000).put("contactAgeMs",contactAge).put("stateAgeMs",stateAge)
   .put("stateFresh",stateAge>=0&&stateAge<=120000).put("waitMs",Math.max(0,Math.min(600000,waitMs))).put("queued",Math.max(0,Math.min(100,queued)));
 }
 private ConversationRuntimeSnapshot(){}
}
