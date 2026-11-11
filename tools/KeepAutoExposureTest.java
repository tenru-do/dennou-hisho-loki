package com.example.rokidkeyboardbridge;
import java.util.*;
public final class KeepAutoExposureTest {
 static int checks;static void ok(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static int color(int y){return 0xff000000|y<<16|y<<8|y;}
 static KeepAutoExposurePolicy.Stats flat(int value){int[] pixels=new int[1000];Arrays.fill(pixels,color(value));return KeepAutoExposurePolicy.pixels(pixels);}
 static void near(float a,float b,String why){ok(Math.abs(a-b)<.001,why+": "+a);}
 public static void main(String[] args){
  near(KeepAutoExposurePolicy.ev(flat(255),10_000_000,100),-1,"clipped sun protects highlights");
  near(KeepAutoExposurePolicy.ev(flat(128),10_000_000,100),0,"normal scene unchanged");
  near(KeepAutoExposurePolicy.ev(flat(20),16_000_000,400),2f/3,"safe dark scene brightened");
  near(KeepAutoExposurePolicy.ev(flat(40),16_000_000,400),1f/3,"moderate shadow correction");
  near(KeepAutoExposurePolicy.ev(flat(20),66_000_000,400),1f/3,"slow shutter limited");
  near(KeepAutoExposurePolicy.ev(flat(20),16_000_000,3200),1f/3,"noisy high ISO limited");
  near(KeepAutoExposurePolicy.ev(flat(20),100_000_000,400),0,"already very slow shutter preserved");
  near(KeepAutoExposurePolicy.ev(flat(20),0,0),1f/3,"unknown metering conservative");
  near(KeepAutoExposurePolicy.ev(flat(0),16_000_000,400),0,"no photons does not amplify black noise");
  ok(KeepAutoExposurePolicy.pixels(null)==null&&KeepAutoExposurePolicy.pixels(new int[63])==null&&KeepAutoExposurePolicy.pixels(new int[25601])==null,"invalid/unbounded thumbnails rejected");
  int[] mixed=new int[1000];Arrays.fill(mixed,color(128));for(int i=0;i<80;i++)mixed[i]=color(240);for(int i=0;i<20;i++)mixed[i]=color(255);
  near(KeepAutoExposurePolicy.ev(KeepAutoExposurePolicy.pixels(mixed),16_000_000,200),-2f/3,"partial near-white clipping corrected");
  Arrays.fill(mixed,color(20));for(int i=0;i<10;i++)mixed[i]=color(255);near(KeepAutoExposurePolicy.ev(KeepAutoExposurePolicy.pixels(mixed),16_000_000,200),2f/3,"few street lights do not darken road");
  for(float step:new float[]{1f/6,.1f,.5f,1f})for(int y=0;y<256;y++)for(long shutter:new long[]{0,8_000_000,40_000_000,120_000_000}){
   float ev=KeepAutoExposurePolicy.ev(flat(y),shutter,400);ok(ev>=-1&&ev<=2f/3,"bounded EV");int value=KeepAutoExposurePolicy.compensation(ev,step,-2,2);ok(value>=-2&&value<=2,"hardware clamp");
  }
  ok(KeepAutoExposurePolicy.compensation(-1,1f/6,-24,24)==-6&&KeepAutoExposurePolicy.compensation(2f/3,1f/6,-24,24)==4,"real glasses capability conversion");
  ok(KeepAutoExposurePolicy.compensation(1,0,0,0)==0&&KeepAutoExposurePolicy.compensation(Float.NaN,1,-1,1)==0,"unsupported exposure safe");
  KeepAutoExposureCycle cycle=new KeepAutoExposureCycle();ok(cycle.needsAnalysis(),"analysis initially available");near(cycle.decide(flat(255),8_000_000,100),-1,"first decision");ok(!cycle.needsAnalysis(),"only one thumbnail analysis");for(int i=0;i<100;i++)near(cycle.decide(flat(20),8_000_000,100),0,"no cumulative correction or oscillation");
  cycle.applied(1000);ok(cycle.correcting()&&cycle.discardSettling(1100)&&cycle.discardSettling(1200)&&!cycle.discardSettling(1300),"at most two warm-up frames after correction");
  KeepAutoExposureCycle deadline=new KeepAutoExposureCycle();deadline.applied(1000);ok(!deadline.discardSettling(1900),"900ms bound");KeepAutoExposureCycle backwards=new KeepAutoExposureCycle();backwards.applied(1000);ok(!backwards.discardSettling(999),"clock anomaly cannot hold capture");
  System.out.println("PASS "+checks+" KEEP adaptive exposure, clipping/shadow, blur/noise limit, range and finite-cycle checks");
 }
}
