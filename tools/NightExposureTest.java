package com.example.rokidkeyboardbridge;
public class NightExposureTest {
 public static void main(String[] args){int n=0;for(int h=0;h<24;h++)for(int y=0;y<256;y++)for(long ns:new long[]{0,16000000,66000000,100000000}){
  KeepAutoExposurePolicy.Stats s=new KeepAutoExposurePolicy.Stats(y,y,y,y>=246?1:0,1000);
  float old=KeepAutoExposurePolicy.ev(s,ns,400),v=KeepAutoExposurePolicy.ev(s,ns,400,h);
  if(v< -1||v>1||(! (h>=17||h<7)&&v!=old)||(y>=170&&v!=old)||(ns>=100000000&&v!=old))throw new AssertionError("unsafe night correction");n++;
 }
 KeepAutoExposurePolicy.Stats dark=new KeepAutoExposurePolicy.Stats(20,30,40,0,1000);
 if(KeepAutoExposurePolicy.ev(dark,16000000,400,22)!=1f||KeepAutoExposurePolicy.ev(dark,66000000,3200,22)>2f/3f)throw new AssertionError("night strength or blur limit");
 System.out.println("PASS "+(n+2)+" night/day, clipping, shutter limits");
 }
}
