package com.example.rokidkeyboardbridge;
public class NightExposureTest {
 public static void main(String[] args){int n=0;for(int h=0;h<24;h++)for(int y=0;y<256;y++)for(long ns:new long[]{0,16000000,66000000,100000000}){
  KeepAutoExposurePolicy.Stats s=new KeepAutoExposurePolicy.Stats(y,y,y,y>=246?1:0,1000);
  float old=KeepAutoExposurePolicy.ev(s,ns,400),v=KeepAutoExposurePolicy.ev(s,ns,400,h);
  for(float step:new float[]{1f/6f,1f/3f,.5f}){
   float boosted=KeepAutoExposurePolicy.ev(s,ns,400,h,step);
   if(boosted< -1||boosted>2||(!(h>=17||h<7)&&boosted!=v)||(y>=170&&boosted!=v)||((ns==0||ns>33333333)&&boosted!=v))throw new AssertionError("unsafe four-step correction");n++;
  }
  if(v< -1||v>1||(! (h>=17||h<7)&&v!=old)||(y>=170&&v!=old)||(ns>=100000000&&v!=old))throw new AssertionError("unsafe night correction");n++;
 }
 KeepAutoExposurePolicy.Stats dark=new KeepAutoExposurePolicy.Stats(20,30,40,0,1000);
 if(KeepAutoExposurePolicy.ev(dark,16000000,400,22)!=1f||KeepAutoExposurePolicy.ev(dark,66000000,3200,22)>2f/3f)throw new AssertionError("night strength or blur limit");
 if(Math.abs(KeepAutoExposurePolicy.ev(dark,16000000,400,22,1f/6f)-5f/3f)>.001f)throw new AssertionError("four real camera steps missing");
 if(KeepAutoExposurePolicy.compensation(4f/3f,1f/6f,-24,24)!=8)throw new AssertionError("stronger correction clamped away");
 for(float step:new float[]{1f/6f,1f/3f}){
  float actual=KeepAutoExposurePolicy.ev(dark,10000000,400,22,step);
  float previous=Math.min(1f+2f*step,2f);
  if(Math.abs(actual-Math.min(previous+2f*step,2f))>.001f)throw new AssertionError("two additional steps relative to v759");
  n++;
 }
 System.out.println("PASS "+(n+2)+" night/day, clipping, shutter limits");
 }
}
