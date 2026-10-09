package com.example.rokidkeyboardbridge;
/** Bounded continuous timeline; no new bitmap frames are allocated. */
final class FullBodyBlendPolicy {
 static final long POSE_MS=360;
 static float ease(float value){float t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
 static float position(long elapsed,long duration,int segments){return Math.min(segments,Math.max(0,elapsed)/(float)duration*segments);}
}
