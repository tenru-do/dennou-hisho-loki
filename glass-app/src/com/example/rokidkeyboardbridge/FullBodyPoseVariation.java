package com.example.rokidkeyboardbridge;

/** Decorate an existing target with a same-posture pose; never schedule standing/sitting. */
final class FullBodyPoseVariation {
 private static final int[] SEATED_IDLE={168,167,168,165},SEATED_THOUGHT={167,168,167,165},STANDING={4,13,4,31,4,37,4,77};
 private int anchor=-1,lastMode=-1;private long started;
 int target(int base,int mode,long now,FullBodyPoseCatalog.Kind speechKind){
  return target(base,mode,now,speechKind,true);
 }
 int target(int base,int mode,long now,FullBodyPoseCatalog.Kind speechKind,boolean uncrossed){
  if(base!=anchor||mode!=lastMode){anchor=base;lastMode=mode;started=now;}
  long interval=mode==2?9000L:mode==1?20000L:15000L;
  int step=mode==0?idleStep(base,Math.max(0,now-started)):(int)(Math.max(0,now-started)/interval);
  if(step==0)return base;
  boolean seated=FullBodyMotionPolicy.isSeated(base);
  if(mode!=2){
   if(seated&&base==168&&!uncrossed)return base;
   int[] pool=seated?(base==167||base==135?SEATED_THOUGHT:SEATED_IDLE):STANDING;
   // Hold the last reaction for one interval, then resume calm same-posture idle poses.
   // Speech keeps its emotion-matched pool below.
   if(mode==0||mode==1)return pool[step%pool.length];
   return base;
  }
  if(speechKind==null)return base;
  FullBodyPoseCatalog.Pose original=FullBodyPoseCatalog.ALL[base];
  int count=0;for(FullBodyPoseCatalog.Pose p:FullBodyPoseCatalog.ALL)
   if(p.seated==seated&&p.supports(speechKind)&&p.intensity==original.intensity)count++;
  if(count<2)return base;
  int selected=step%count;
  for(FullBodyPoseCatalog.Pose p:FullBodyPoseCatalog.ALL)
   if(p.seated==seated&&p.supports(speechKind)&&p.intensity==original.intensity&&selected--==0)return p.id;
  return base;
 }
 // Brief rest for neutral poses; retain strong conversation reactions for 15 seconds.
 private static int idleStep(int base,long elapsed){
  long hold=(base==168||base==169||base==4)?4000L:15000L;
  if(elapsed<hold)return 0;
  long time=elapsed-hold;int[] durations={6200,7400,5600,6800,6100};
  long cycle=32100L;int step=1+(int)(time/cycle)*durations.length;time%=cycle;
  for(int duration:durations){if(time<duration)return step;time-=duration;step++;}
  return step;
 }
 int anchorPose(){return anchor;}
 void reset(){anchor=lastMode=-1;started=0;}
}

