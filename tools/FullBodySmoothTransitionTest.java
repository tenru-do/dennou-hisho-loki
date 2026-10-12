package com.example.rokidkeyboardbridge;
public final class FullBodySmoothTransitionTest {
 public static void main(String[]args){int checks=0;
  for(int link=0;link<FullBodyPoseLinks.PAIRS.length;link++){
   int[] ids=FullBodyPoseLinks.frames(link);if(link>=4&&ids.length!=12)throw new AssertionError("Sparse route");
   boolean[] seen=new boolean[FullBodyRealisticFeatures.ALL.length];
   for(int id:ids){if(id<0||id>=seen.length||seen[id])throw new AssertionError("Invalid/repeated frame");seen[id]=true;}
   for(boolean posture:new boolean[]{false,true}){
    long duration=FullBodySmoothTransitionPolicy.duration(ids.length,posture);float old=0;
    for(long t=0;t<=duration;t++){
     float p=FullBodySmoothTransitionPolicy.position(t,duration,ids.length);
     if(p<old||p>ids.length+1||p-old>.04f)throw new AssertionError("Timeline jump");
     if(t>duration/4&&t<duration*3/4&&p<=old)throw new AssertionError("Route stops between frames");
     old=p;checks++;
    }
    if(old!=ids.length+1||FullBodySmoothTransitionPolicy.position(-1,duration,ids.length)!=0)throw new AssertionError("Endpoint");
   }
  }
  System.out.println("PASS "+checks+" dense route acceleration/continuity checks, six bidirectional paths");
 }
}
