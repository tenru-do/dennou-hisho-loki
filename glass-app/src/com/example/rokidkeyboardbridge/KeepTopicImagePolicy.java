package com.example.rokidkeyboardbridge;
/** Optional TOPIC vision may never delay KEEP capture/save or expand an old snapshot. */
final class KeepTopicImagePolicy {
 static String reason(long now,long capturedAt,long previousUse,long sourceBytes,long freeHeap,boolean lowMemory,boolean keepWaiting){
  if(keepWaiting)return "KEEP_WAIT";if(lowMemory)return "LOW_MEMORY";if(capturedAt<=0)return "NO_FRAME";if(now<capturedAt)return "CLOCK";if(now-capturedAt>15000)return "STALE";
  if(previousUse!=0&&(now<previousUse||now-previousUse<30000))return "RATE";if(sourceBytes<=0)return "MISSING";if(sourceBytes>3L*1024*1024)return "SOURCE_SIZE";if(freeHeap<32L*1024*1024+sourceBytes*4)return "HEAP_RESERVE";return "";
 }
 static boolean canReuse(long now,long capturedAt,long previousUse,long sourceBytes,long freeHeap,boolean lowMemory,boolean keepWaiting){return reason(now,capturedAt,previousUse,sourceBytes,freeHeap,lowMemory,keepWaiting).isEmpty();}
}
