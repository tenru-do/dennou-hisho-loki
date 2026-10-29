package com.example.rokidkeyboardbridge;
/** Bounded, visual-only gestures. Semantic expressions always take precedence. */
final class PortraitLifeMotion {
 static final long PERIOD=18000L, START=3000L, DURATION=2400L;
 static float pulse(long time,long period,long start,long duration){
  long t=Math.max(0,time)%period-start;
  if(t<=0||t>=duration)return 0;
  float s=(float)Math.sin(Math.PI*t/duration);return s*s;
 }
 static float gesture(long now,int mode){return mode==1?pulse(now,6500,800,1800):mode==2?pulse(now,4700,600,1300):pulse(now,PERIOD,START,DURATION);}
 static int kind(long now){return CharacterMotionTuning.variant(Math.max(0,now)/PERIOD);}
 static boolean neutral(int face){return face==0||face==14;}
 static int face(int original,long now,int mode,boolean quiet){
  return !quiet&&mode==0&&neutral(original)&&kind(now)==2&&gesture(now,mode)>.15f?1:original;
 }
 static float tilt(long now,int mode,boolean quiet){
  if(quiet)return 0;
  float p=gesture(now,mode);
  return mode==1?-2.8f*p:mode==2?.8f*p:(kind(now)==0?-3f:kind(now)==1?2.2f:kind(now)==3?-1.4f:.7f)*p;
 }
 static float nod(long now,int mode,boolean quiet){
  if(quiet)return 0;
  return gesture(now,mode)*(mode==1?.009f:mode==2?.012f:kind(now)==1?.018f:.005f);
 }
 static float gaze(long now,int mode,boolean quiet){
  if(quiet||mode==2)return 0;
  return gesture(now,mode)*(mode==1?-.005f:kind(now)==0?-.009f:kind(now)==3?.009f:0);
 }
 static long delay(long now,int mode,boolean quiet){
  if(mode==2)return quiet?125:80;
  long blink=Math.max(0,now)%4100;
  if(blink<280)return quiet?80:40;
  if(!quiet&&gesture(now,mode)>0)return 66;
  return Math.max(quiet?80:40,Math.min(quiet?400:250,4100-blink));
 }
}
