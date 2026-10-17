package com.example.rokidkeyboardbridge;
/** Clamp relative movement to available media bounds. */
final class MusicSeekPolicy {
 static long target(long position,long delta,long duration){
  long value=delta>0&&position>Long.MAX_VALUE-delta?Long.MAX_VALUE:position+delta;
  value=Math.max(0,value);return duration>0?Math.min(value,duration):value;
 }
}
