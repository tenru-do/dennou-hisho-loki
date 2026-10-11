package com.example.rokidkeyboardbridge;
public final class FullBodyDramaticMotionTest {
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
 static float[] weights(long t,long period){
  float[] w=new float[8];int a,b;float blend;
  if(FullBodyDramaticMotionPolicy.bridge(t)){a=FullBodyDramaticMotionPolicy.bridgeFrom(t);b=FullBodyDramaticMotionPolicy.bridgeTo(t);blend=FullBodyDramaticMotionPolicy.bridgeBlend(t);}
  else{long local=FullBodyDramaticMotionPolicy.holdElapsed(t);int base=FullBodyDramaticMotionPolicy.second(t)?3:0;a=base+FullBodyDramaticMotionPolicy.breathFrom(local,period);b=base+FullBodyDramaticMotionPolicy.breathTo(local,period);blend=FullBodyDramaticMotionPolicy.breathBlend(local,period);}
  check(a>=0&&a<8&&b>=0&&b<8&&blend>=0&&blend<=1,"frame/alpha bounds");w[a]=1-blend;w[b]+=blend;return w;
 }
 public static void main(String[] args){
  check(FullBodyDramaticMotionPolicy.period(0)==1800&&FullBodyDramaticMotionPolicy.period(4)==900,"context controls pace");
  for(long period:new long[]{900,1800}){
   float[] previous=weights(0,period);
   for(long t=1;t<=FullBodyDramaticMotionPolicy.STRESS_CYCLE_MS*2;t++){
    float[] current=weights(t,period);
    for(int i=0;i<8;i++)check(Math.abs(current[i]-previous[i])<.01,"continuous frame weights at "+t);
    previous=current;
   }
   check(weights(0,period)[0]==1&&weights(12000,period)[0]==1,"cycle seam");
  }
  check(FullBodyDramaticFeatures.ALL.length==8,"catalog");
  check(FullBodyFaceAnimator.blink(4499)==0&&FullBodyFaceAnimator.blink(4580)==1&&FullBodyFaceAnimator.blink(4740)==0,"blink endpoints");
  check(FullBodyApprovedFeatures.ALL.length==8,"existing motion face catalog");
  for(float[] f:FullBodyApprovedFeatures.ALL){
   check(f!=null&&f.length==19,"existing face format");
   for(float v:f)check(Float.isFinite(v),"finite existing face");
   for(int o:new int[]{2,8,14})check(f[o]>0&&f[o]<f[0]&&f[o+1]>0&&f[o+1]<f[1],"existing face bounds");
  }
  for(int i=6;i<8;i++)check(FullBodyApprovedFeatures.ALL[i][7]==0&&FullBodyApprovedFeatures.ALL[i][13]==0&&FullBodyApprovedFeatures.ALL[i][16]==0,"closed eyes and covered mouth preserved");
  for(int scene=0;scene<8;scene++){
   int count=scene==7?8:scene==6?2:3;
   check(FullBodyDramaticFeatures.ALL[scene].length==count,"feature/frame count");
   check(FullBodyDramaticFeatures.SHIFTX[scene].length==count&&FullBodyDramaticFeatures.SHIFTY[scene].length==count,"anchor/frame count");
   for(float[] f:FullBodyDramaticFeatures.ALL[scene]){
    check(f.length==19&&f[0]>0&&f[1]>0,"feature format");
    for(float v:f)check(Float.isFinite(v),"finite landmarks");
    for(int o:new int[]{2,8,14})check(f[o]>0&&f[o]<f[0]&&f[o+1]>0&&f[o+1]<f[1]&&f[o+2]>0,"face bounds");
   }
  }
  System.out.println("PASS dramatic motion: 48000 timeline steps, both breathing speeds, real intermediate frame ranges, continuous weights and face metadata");
 }
}
