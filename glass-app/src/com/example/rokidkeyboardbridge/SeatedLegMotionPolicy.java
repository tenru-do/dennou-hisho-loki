package com.example.rokidkeyboardbridge;

/**
 * Bounded, caller-driven ordinary seated leg motion. No timers, IO or scene mutation.
 * Atlas order: right crossed/resting, right ankle lift, uncrossing, feet together,
 * left knee lift, left crossing, left lowering, left crossed/resting.
 */
final class SeatedLegMotionPolicy {
 static final int RIGHT=0,LEFT=1,NEUTRAL=2,FRAMES=8;
 static final long DURATION=7200,AUTOMATIC_INTERVAL=120000,FADE=240;
 private static final long[] STEP_MS={700,900,1000,1050,950,950,1650};
 private int side=RIGHT,fromFrame=0,targetFrame=7,pendingTarget=7;
 private long duration=DURATION;
 private boolean running,pending,pendingManual,runningManual,paused,previousAllowed;
 private long elapsed,idleElapsed,lastUpdate=-1;

 /** Queues one request; repeated commands never restart or queue another motion. */
 boolean request(long now,boolean manual){
  return requestTarget(now,manual,side==RIGHT?LEFT:RIGHT);
 }
 boolean requestTarget(long now,boolean manual,int target){
  if(target<RIGHT||target>NEUTRAL||now<0||running)return false;
  if(pending){if(manual&&!pendingManual){pendingManual=true;return true;}return false;}
  pendingTarget=target==RIGHT?0:target==LEFT?7:3;
  if(pendingTarget==frame())return false;
  pending=true;pendingManual=manual;paused=false;lastUpdate=now;previousAllowed=false;
  return true;
 }

 /**
  * Uses only nondecreasing caller uptime. Quiet/held/speaking pauses retain the
  * current frame and do not consume playback time. Automatic motion requires
  * 120 seconds of uninterrupted eligible idle time, including queued auto work.
  */
 void update(long now,boolean seated,boolean quiet,boolean held,boolean speaking,boolean natural){
  if(now<0)return;
  boolean backwards=lastUpdate>=0&&now<lastUpdate;
  long delta=lastUpdate<0||backwards?0:now-lastUpdate;
  boolean allowed=seated&&!quiet&&!held&&!speaking;
  boolean advance=allowed&&previousAllowed&&!backwards;
  lastUpdate=now;previousAllowed=allowed;
  if(!seated){cancel(now);return;}
  if(!natural){
   idleElapsed=0;
   if(running&&!runningManual||pending&&!pendingManual){cancel(now);return;}
  }
  if(running){
   paused=!allowed;
   if(advance)elapsed=boundedAdd(elapsed,delta,duration);
   if(elapsed>=duration){
    side=targetFrame==0?RIGHT:targetFrame==7?LEFT:NEUTRAL;
    running=runningManual=paused=false;elapsed=idleElapsed=0;
   }
   return;
  }
  paused=pending&&!allowed;
  if(!allowed){idleElapsed=0;return;}
  if(natural&&advance)idleElapsed=boundedAdd(idleElapsed,delta,AUTOMATIC_INTERVAL);
  if(pending&&pendingManual||natural&&idleElapsed>=AUTOMATIC_INTERVAL){
   fromFrame=frame();targetFrame=pending?pendingTarget:side==RIGHT?7:0;
   duration=0;for(int i=0;i<Math.abs(targetFrame-fromFrame);i++)duration+=STEP_MS[i];
   running=true;runningManual=pending&&pendingManual;
   pending=pendingManual=paused=false;elapsed=idleElapsed=0;
  }
 }

 private static long boundedAdd(long value,long delta,long maximum){
  return value+Math.min(Math.max(0,delta),maximum-value);
 }
 private int step(){long age=elapsed;for(int i=0;i<STEP_MS.length;i++){if(age<STEP_MS[i])return i;age-=STEP_MS[i];}return FRAMES-2;}
 private long stepAge(){long age=elapsed;for(long duration:STEP_MS){if(age<duration)return age;age-=duration;}return STEP_MS[STEP_MS.length-1];}
 private int source(int step){return fromFrame+(targetFrame>fromFrame?step:-step);}
 int frame(){return running?source(step()):side==RIGHT?0:side==LEFT?FRAMES-1:3;}
 int nextFrame(){return running?source(step()+1):frame();}
 float blend(){
  if(!running)return 0;
  long duration=STEP_MS[step()];
  float value=Math.max(0,Math.min(1,stepAge()/(float)duration));
  return value*value*(3-2*value);
 }
 boolean active(){return running;}
 boolean waiting(){return pending;}
 boolean paused(){return paused;}
 int settledSide(){return side;}

 /** Drops all work and returns to the last completed endpoint; nothing can resume. */
 void cancel(long now){
  running=pending=pendingManual=runningManual=paused=previousAllowed=false;
  elapsed=idleElapsed=0;lastUpdate=now<0?-1:now;
 }
}
