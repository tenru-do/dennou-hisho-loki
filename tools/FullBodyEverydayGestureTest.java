package com.example.rokidkeyboardbridge;
public final class FullBodyEverydayGestureTest {
 static int checks;static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  for(boolean seated:new boolean[]{false,true}){
   int ears=0;
   for(long t=0;t<240000;t+=100){int clip=FullBodyEverydayGesture.automatic(t,seated,false);if(clip>=0){ok(clip%2==(seated?1:0),"posture retained");int frame=FullBodyEverydayGesture.frame(clip,FullBodyEverydayGesture.local(t,clip));ok(frame>=0&&frame<(clip>=2?3:4),"registered frame only");if(clip>=2)ears++;}}
   ok(ears==92,"ear habit only 4.6 seconds per two minutes");
  }
  for(int clip=0;clip<4;clip++){
   boolean[] seen=new boolean[clip>=2?3:4];for(long t=0;t<4600;t+=25){int frame=FullBodyEverydayGesture.frame(clip,t);seen[frame]=true;}
   for(boolean b:seen)ok(b,"all selected frames reachable");ok(FullBodyEverydayGesture.frame(clip,5000)==0,"settles at rest");
  }
  for(FullBodyPoseCatalog.Kind kind:FullBodyPoseCatalog.Kind.values()){
   ok(!FullBodyEverydayGesture.allowed(1,true,kind)&&!FullBodyEverydayGesture.allowed(2,true,kind),"listening/speech preempt");
  }
  for(FullBodyPoseCatalog.Kind kind:new FullBodyPoseCatalog.Kind[]{FullBodyPoseCatalog.Kind.REFUSE,FullBodyPoseCatalog.Kind.ANGER,FullBodyPoseCatalog.Kind.WORRY,FullBodyPoseCatalog.Kind.SAD,FullBodyPoseCatalog.Kind.STRAIN})ok(!FullBodyEverydayGesture.allowed(0,true,kind),"strong emotion retained");
  ok(FullBodyEverydayGesture.automatic(12001,false,true)==-1,"thinking excludes casual wave");ok(FullBodyEverydayGesture.automatic(50001,true,true)==3,"thinking allows seated ear habit");
  System.out.println("PASS "+checks+" posture/ear frequency/frame boundaries/emotion precedence assertions");
 }
}
