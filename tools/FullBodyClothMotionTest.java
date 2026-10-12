package com.example.rokidkeyboardbridge;
public final class FullBodyClothMotionTest {
 public static void main(String[] args){int cases=0;
  if(FullBodyClothMotion.group(168)!=3)throw new AssertionError("Seated group missing");
  for(long t=0;t<120000;t++){float x=FullBodyClothMotion.position(t,3);if(x<0||x>=12)throw new AssertionError("Seated timeline bounds");}
  for(int n=0;n<=12;n++){int id=FullBodyClothMotion.pose(3,n);if(FullBodyClothMotion.base(id)!=168)throw new AssertionError("Seated posture changed");}
  if(FullBodyClothMotion.position(24000,3)!=6||FullBodyClothMotion.position(80000,3)!=0)throw new AssertionError("No quiet seated hold");
  if(FullBodyClothMotion.group(13)!=-1||FullBodyClothMotion.group(0)!=-1)throw new AssertionError("Unrelated pose replaced");
  for(int base:new int[]{4,1,2}){
   int group=FullBodyClothMotion.group(base);if(group<0)throw new AssertionError("Missing group");
   float prev=0;
   for(long t=0;t<8400;t++){
    float x=FullBodyClothMotion.position(t);if(x<prev||x-prev>.002||x<0||x>=7)throw new AssertionError("Cloth timeline jump");prev=x;cases++;
   }
   if(FullBodyClothMotion.pose(group,0)!=base||FullBodyClothMotion.pose(group,7)!=base||FullBodyClothMotion.position(8400)!=0)throw new AssertionError("Cycle does not return to selected pose");
   for(int n=1;n<=6;n++){int pose=FullBodyClothMotion.pose(group,n);
    if(FullBodyClothMotion.base(pose)!=base||FullBodyFaceAnimator.transition(pose))throw new AssertionError("Lost held pose or face motion");
    if(FullBodyLivingPosePolicy.profile(pose,FullBodyRealisticFeatures.ALL[pose])!=FullBodyLivingPosePolicy.profile(base,FullBodyRealisticFeatures.ALL[base]))throw new AssertionError("Living profile lost");
   }
  }
  for(int height:new int[]{299,601}){float[] ref=FullBodyRealisticFeatures.ALL[168];float expected=ref[1]*FullBodyScalePolicy.scale(168,(int)ref[0],(int)ref[1],240,height);for(int id=359;id<=364;id++){float[] f=FullBodyRealisticFeatures.ALL[id];float actual=f[1]*FullBodyScalePolicy.scale(id,(int)f[0],(int)f[1],240,height);if(actual<expected*.94f||actual>expected*1.06f)throw new AssertionError("Seated scale jump "+id+" "+actual+" vs "+expected);}}
  System.out.println("PASS "+cases+" cloth cycle timing checks; 24 frames preserve selected pose, face animation and breathing profile");
 }
}
