package com.example.rokidkeyboardbridge;
/** One acceleration envelope for the whole route, avoiding stop/start at every drawing. */
final class FullBodySmoothTransitionPolicy {
 static long duration(int frames,boolean posture){return Math.max(posture?1200L:900L,(frames+1)*90L);}
 static float position(long elapsed,long duration,int frames){
  double t=Math.max(0,Math.min(1,elapsed/(double)Math.max(1,duration)));
  double smooth=t*t*t*(t*(t*6-15)+10);
  return (float)Math.max(0,Math.min(frames+1,smooth*(frames+1)));
 }
}
