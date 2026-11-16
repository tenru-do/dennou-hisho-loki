package com.example.rokidkeyboardbridge;
/** Bounded feedback from one metered thumbnail. No cumulative EV or archived-image edits. */
final class KeepAutoExposurePolicy {
 static final class Stats {
  final int median,p90,p95;final float clipped;final int samples;
  Stats(int median,int p90,int p95,float clipped,int samples){this.median=median;this.p90=p90;this.p95=p95;this.clipped=clipped;this.samples=samples;}
 }
 static Stats pixels(int[] pixels){
  if(pixels==null||pixels.length<64||pixels.length>25600)return null;
  int[] histogram=new int[256];int clipped=0;
  for(int color:pixels){int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255,y=(54*r+183*g+19*b+128)>>8;histogram[Math.min(255,y)]++;if(y>=246||(Math.max(r,Math.max(g,b))>=253&&y>=218))clipped++;}
  return new Stats(percentile(histogram,pixels.length,50),percentile(histogram,pixels.length,90),percentile(histogram,pixels.length,95),clipped/(float)pixels.length,pixels.length);
 }
 private static int percentile(int[] h,int count,int percent){int target=(count*percent+99)/100,sum=0;for(int i=0;i<h.length;i++){sum+=h[i];if(sum>=target)return i;}return 255;}
 static float ev(Stats s,long exposureNs,int iso){
  if(s==null||s.samples<64||!Float.isFinite(s.clipped))return 0;
  // Protect bright detail first; a few night lights must not darken the whole road.
  if(s.clipped>=.08f&&s.p90>=220)return -1f;
  if(s.clipped>=.015f&&s.p95>=235)return -2f/3f;
  if(s.median>=190&&s.p90>=235)return -1f/3f;
  if(s.median>=45||s.p90>=150||s.clipped>=.02f||s.p90<4)return 0;
  // Do not extend an already very slow shutter. Unknown/very noisy metering gets only +1/3 EV.
  if(exposureNs>=100_000_000L)return 0;
  float limit=exposureNs<=0||iso<=0||exposureNs>33_333_333L||iso>=1600?1f/3f:2f/3f;
  return Math.min(s.median<24?2f/3f:1f/3f,limit);
 }
 static float ev(Stats s,long exposureNs,int iso,int hour){
  float base=ev(s,exposureNs,iso);
  // Night bias only when measured shadows justify it. Preserve clipping and motion limits.
  if(hour<0||hour>23||!(hour>=17||hour<7)||s==null||s.samples<64||!Float.isFinite(s.clipped)
    ||base<0||s.median>=60||s.p90>=170||s.p90<4||s.clipped>=.02f||exposureNs>=100_000_000L)return base;
  float limit=exposureNs<=0||iso<=0||exposureNs>33_333_333L||iso>=1600?2f/3f:1f;
  return Math.max(base,Math.min(s.median<30?1f:2f/3f,limit));
 }
 static float ev(Stats s,long exposureNs,int iso,int hour,float step){
  float base=ev(s,exposureNs,iso,hour);
  // Four steps above the original night bias: two more than v759.
  // Keep slow/noisy or unknown metering at the existing conservative setting.
  if(hour<0||hour>23||!(hour>=17||hour<7)||s==null||s.samples<64||!Float.isFinite(s.clipped)
    ||!Float.isFinite(step)||step<=0||base<0||s.median>=60||s.p90>=170||s.p90<4||s.clipped>=.02f
    ||exposureNs<=0||exposureNs>33_333_333L||iso<=0||iso>=1600)return base;
  float shutterLimit=(float)(Math.log(100_000_000d/exposureNs)/Math.log(2d));
  return Math.max(base,Math.min(base+4f*step,Math.min(2f,shutterLimit)));
 }
 static int compensation(float ev,float step,int minimum,int maximum){if(!Float.isFinite(ev)||!Float.isFinite(step)||step<=0||minimum>maximum)return 0;return Math.max(minimum,Math.min(maximum,Math.round(Math.max(-1f,Math.min(2f,ev))/step)));}
 private KeepAutoExposurePolicy(){}
}
