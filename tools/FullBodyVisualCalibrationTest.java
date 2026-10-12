package com.example.rokidkeyboardbridge;
public final class FullBodyVisualCalibrationTest{
 public static void main(String[]args){
  int checked=0;
  for(int height:new int[]{299,598,640})for(int link:new int[]{4,5}){
   int from=FullBodyPoseLinks.PAIRS[link][0],to=FullBodyPoseLinks.PAIRS[link][1];
   float[] a=FullBodyRealisticFeatures.ALL[from],b=FullBodyRealisticFeatures.ALL[to];
   float sourceHeight=a[1]*FullBodyScalePolicy.scale(from,(int)a[0],(int)a[1],240,height),targetHeight=b[1]*FullBodyScalePolicy.scale(to,(int)b[0],(int)b[1],240,height);
   for(int id:FullBodyPoseLinks.frames(link)){
    float[]f=FullBodyRealisticFeatures.ALL[id];float actual=f[1]*FullBodyScalePolicy.scale(id,(int)f[0],(int)f[1],240,height);
    if(actual<Math.min(sourceHeight,targetHeight)*.94f||actual>Math.max(sourceHeight,targetHeight)*1.06f)throw new AssertionError("Body shrinks or grows during transition: "+id+" height="+height);
    checked++;
   }
  }
  for(long t=0;t<5280;t++){
   float value=FullBodyFaceAnimator.mouth(t,true),next=FullBodyFaceAnimator.mouth(t+1,true);
   if(value<0||value>1||Math.abs(next-value)>.015f||FullBodyFaceAnimator.mouth(t,false)!=0)throw new AssertionError("Lip discontinuity or silent mouth");
  }
  if(FullBodyPoseLinks.index(13,1)>=0||FullBodyPoseLinks.index(1,13)>=0)throw new AssertionError("Chin pose sent through arms-down sequence");
  System.out.println("PASS "+checked+" endpoint-relative body-scale checks; 5280 lip interpolation/silence checks; chin alias excluded");
 }
}
