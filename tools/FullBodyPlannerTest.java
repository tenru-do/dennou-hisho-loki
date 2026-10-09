package com.example.rokidkeyboardbridge;
/** New activity assets retain posture, body scale and full head-to-shoe visibility. */
public final class FullBodyPlannerTest {
 static void check(boolean v,String text){if(!v)throw new AssertionError(text);}
 public static void main(String[]args){
  check(FullBodyRealisticAssets.REGIONS.length==276,"planner assets appended");
  for(boolean seated:new boolean[]{false,true}){
   int kind=seated?5:4,reference=seated?168:4;
   check(FullBodyIdleActivity.kind(59999,seated)!=kind,"planner does not start early");
   check(FullBodyIdleActivity.kind(60000,seated)==kind&&FullBodyIdleActivity.kind(89999,seated)==kind,"planner posture");
   check(FullBodyIdleActivity.kind(90000,seated)!=kind,"planner ends");
   java.util.Set<Integer> seen=new java.util.HashSet<>();for(long t=60000;t<90000;t+=10)seen.add(FullBodyIdleActivity.frame(kind,t));check(seen.size()==6,"all planner keyframes reachable");
   float[] ref=FullBodyRealisticFeatures.ALL[reference];float refScale=FullBodyScalePolicy.scale(reference,(int)ref[0],(int)ref[1],240,598);
   for(int pose:FullBodyIdleActivity.frames(kind)){
    float[] f=FullBodyRealisticFeatures.ALL[pose];float scale=FullBodyScalePolicy.scale(pose,(int)f[0],(int)f[1],240,598);
    check(Math.abs(f[1]*scale-ref[1]*refScale)<1,"same endpoint body height "+pose);
    check(f[1]*scale<=598.01f,"head/shoes inside vertical bounds "+pose);
    float left=FullBodyAnchorPolicy.left(pose,(int)f[0],240,scale,seated?.76f:1);
    check(left+FullBodyAnchorPolicy.MIN[pose]*scale>=-.01f&&left+FullBodyAnchorPolicy.MAX[pose]*scale<=240.01f,"horizontal fit "+pose);
    check(FullBodyFaceAnimator.supports(pose)&&!FullBodyFaceAnimator.transition(pose),"face animation available "+pose);
   }
  }
  check(FullBodyIdleActivity.kind(15000,true)==0&&FullBodyIdleActivity.kind(105000,true)==1,"tablet/coffee preserved");
  check(!FullBodyIdleActivity.automaticPenEnabled,"manual pen pickup preference preserved");
  check(FullBodyIdleActivity.firstFrame(2)==234&&FullBodyIdleActivity.firstFrame(3)==246,"existing seated/standing pickup preserved");
  System.out.println("PASS planner: 12 reachable frames; standing/seated preserved; head/shoe and width bounds; calibrated endpoint height; blink/mouth landmarks; existing tablet/coffee and manual pickup intact");
 }
}
