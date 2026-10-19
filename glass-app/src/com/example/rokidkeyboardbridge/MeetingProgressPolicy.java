package com.example.rokidkeyboardbridge;
import org.json.*;
final class MeetingProgressPolicy{
 static JSONObject snapshot(long captured,long written,long sealed,int queued,long elapsed,int level)throws Exception{captured=Math.max(0,captured);written=Math.max(0,Math.min(captured,written));sealed=Math.max(0,Math.min(written,sealed));return new JSONObject().put("capturedMs",captured/32).put("writtenMs",written/32).put("sealedMs",sealed/32).put("pendingMs",(captured-written)/32).put("queuedFrames",Math.max(0,Math.min(64,queued))).put("elapsedMs",Math.max(0,elapsed)).put("inputLevel",Math.max(0,level));}
 private MeetingProgressPolicy(){}
}
