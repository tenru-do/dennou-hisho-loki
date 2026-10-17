package com.example.rokidkeyboardbridge;
/** Optional TOPIC vision may never delay KEEP capture/save or expand an old snapshot. */
final class KeepTopicImagePolicy {
 static boolean canReuse(long now,long capturedAt,long previousUse,long sourceBytes,long freeHeap,boolean lowMemory,boolean keepWaiting) {
  return !keepWaiting&&!lowMemory&&capturedAt>0&&now>=capturedAt&&now-capturedAt<=15000
    &&(previousUse==0||now>=previousUse&&now-previousUse>=30000)
    &&sourceBytes>0&&sourceBytes<=3L*1024*1024&&freeHeap>=32L*1024*1024+sourceBytes*4;
 }
}
