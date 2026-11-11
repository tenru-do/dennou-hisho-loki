package com.example.rokidkeyboardbridge;

/** Pure lifecycle/timeline regressions; no Android framework or artwork decoding. */
public final class SeatedLegMotionPolicyTest {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void update(SeatedLegMotionPolicy p,long now){p.update(now,true,false,false,false,false);}
 private static SeatedLegMotionPolicy start(long now){
  SeatedLegMotionPolicy p=new SeatedLegMotionPolicy();
  check(p.request(now,true),"First manual request accepted");update(p,now);
  check(p.active()&&!p.waiting()&&!p.paused(),"Manual request begins on an eligible caller update");
  return p;
 }
 private static void close(float actual,float expected,String message){check(Math.abs(actual-expected)<.0001f,message);}
 private static void directionAndFinish(){
  SeatedLegMotionPolicy p=start(1000);
  check(p.frame()==0&&p.nextFrame()==1&&p.settledSide()==SeatedLegMotionPolicy.RIGHT,"Forward begins at the completed right endpoint");
  boolean[] forward=new boolean[8],reverse=new boolean[8];
  for(int age=0;age<SeatedLegMotionPolicy.DURATION;age+=37){
   update(p,1000+age);int frame=p.frame(),next=p.nextFrame();forward[frame]=true;forward[next]=true;
   check(frame>=0&&next<8&&next==frame+1,"Forward only uses adjacent chronological atlas frames");
   check(Float.isFinite(p.blend())&&p.blend()>=0&&p.blend()<=1,"Forward blend stays bounded");
  }
  update(p,1000+SeatedLegMotionPolicy.DURATION);
  check(!p.active()&&p.frame()==7&&p.nextFrame()==7&&p.settledSide()==SeatedLegMotionPolicy.LEFT,"Forward completion holds the left endpoint");
  for(boolean seen:forward)check(seen,"Every forward intermediate frame is reachable");
  close(p.blend(),0,"Finished endpoint has no residual fade");
  check(p.request(9000,true),"A new command after completion is accepted");update(p,9000);
  for(int age=0;age<SeatedLegMotionPolicy.DURATION;age+=41){
   update(p,9000+age);int frame=p.frame(),next=p.nextFrame();reverse[frame]=true;reverse[next]=true;
   check(frame<=7&&next>=0&&next==frame-1,"Reverse traverses the same images without skipping an intermediate");
  }
  update(p,9000+SeatedLegMotionPolicy.DURATION);
  for(boolean seen:reverse)check(seen,"Every reverse intermediate frame is reachable");
  check(!p.active()&&p.frame()==0&&p.settledSide()==SeatedLegMotionPolicy.RIGHT,"Reverse finishes at the right endpoint");
  update(p,Long.MAX_VALUE);
  check(!p.active()&&p.frame()==0,"A finished manual motion never loops without another request");
 }
 private static void noRestart(){
  SeatedLegMotionPolicy p=start(100),control=start(100);
  update(p,1100);update(control,1100);
  check(!p.request(1200,true)&&!p.request(1250,false),"Duplicate manual/automatic requests cannot restart active motion");
  update(p,1500);update(control,1500);
  check(p.frame()==control.frame()&&p.nextFrame()==control.nextFrame(),"Duplicate requests preserve the timeline");
  close(p.blend(),control.blend(),"Duplicate requests preserve blend progress");
  update(p,100+SeatedLegMotionPolicy.DURATION);
  check(!p.active()&&!p.waiting(),"Duplicates cannot silently queue another motion");
 }
 private static void pauseAndResume(){
  for(int gate=0;gate<3;gate++){
   SeatedLegMotionPolicy p=start(0);update(p,650);int frame=p.frame(),next=p.nextFrame();float blend=p.blend();
   p.update(1000,true,gate==0,gate==1,gate==2,false);
   check(p.active()&&p.paused(),"Each quiet/held/speaking gate pauses an active motion");
   p.update(50000,true,gate==0,gate==1,gate==2,false);
   check(p.frame()==frame&&p.nextFrame()==next,"Paused time never changes the current image");
   close(p.blend(),blend,"Paused time never consumes fade progress");
   update(p,100000);
   check(!p.paused()&&p.frame()==frame,"Removing the gate does not catch up hidden elapsed time");
   close(p.blend(),blend,"Resume begins at the retained blend");
   update(p,100050);
   check(p.frame()==1,"Subsequent eligible time advances normally");
  }
 }
 private static void queuedManualAndCancel(){
  SeatedLegMotionPolicy p=new SeatedLegMotionPolicy();
  check(p.request(100,true),"Blocked manual command can be queued");
  p.update(100,true,false,false,true,false);
  check(p.waiting()&&p.paused()&&!p.active(),"Queued manual request waits while speaking");
  check(!p.request(500,true),"Repeated queued manual request is not duplicated");
  p.update(10000,true,false,true,false,false);
  check(p.waiting()&&!p.active(),"Hold also delays a queued manual motion");
  update(p,20000);check(p.active()&&p.frame()==0,"Manual starts after the gate even with natural motion disabled");
  update(p,20500);p.cancel(21000);
  check(!p.active()&&!p.waiting()&&!p.paused()&&p.frame()==0,"Cancel restores the completed endpoint and clears all work");
  update(p,Long.MAX_VALUE);
  check(!p.active()&&!p.waiting()&&p.frame()==0,"Cancelled manual work cannot resume on later updates");
  check(p.request(10,true),"Explicit new command remains possible after a caller clock reset");p.cancel(10);update(p,1000);
  check(!p.active()&&!p.waiting(),"Cancel also removes a request that had not started");
 }
 private static void automaticCadence(){
  SeatedLegMotionPolicy p=new SeatedLegMotionPolicy();
  p.update(0,true,false,false,false,true);
  p.update(119999,true,false,false,false,true);
  check(!p.active(),"Automatic movement waits at least 120 seconds");
  p.update(120000,true,false,false,false,true);
  check(p.active()&&p.frame()==0,"Eligible natural idle starts one automatic transition");
  p.update(127200,true,false,false,false,true);
  check(!p.active()&&p.frame()==7,"Automatic transition finishes once and holds its endpoint");
  p.update(247199,true,false,false,false,true);
  check(!p.active(),"Next automatic motion waits 120 seconds after finishing");
  p.update(247200,true,false,false,false,true);
  check(p.active()&&p.frame()==7&&p.nextFrame()==6,"Next automatic motion alternates direction");
  p.update(247300,true,false,false,false,false);
  check(!p.active()&&!p.waiting()&&p.frame()==7,"Disabling natural motion cancels automatic work");

  SeatedLegMotionPolicy blocked=new SeatedLegMotionPolicy();
  check(blocked.request(0,false),"Automatic request can wait for its idle budget");
  blocked.update(0,true,false,false,false,true);blocked.update(100000,true,false,false,false,true);
  check(!blocked.active()&&blocked.waiting(),"Queued automatic request cannot bypass the interval");
  blocked.update(110000,true,true,false,false,true);blocked.update(900000,true,true,false,false,true);
  blocked.update(1000000,true,false,false,false,true);blocked.update(1119999,true,false,false,false,true);
  check(!blocked.active(),"Quiet interval resets eligible idle instead of creating an immediate burst");
  blocked.update(1120000,true,false,false,false,true);
  check(blocked.active(),"Automatic waiting eventually progresses after a fresh idle interval");
 }
 private static void clockAndPosture(){
  SeatedLegMotionPolicy p=start(1000);update(p,1650);int frame=p.frame();float blend=p.blend();
  update(p,100);
  check(p.frame()==frame,"Backward caller clock never rewinds frame order");close(p.blend(),blend,"Backward caller clock never rewinds blend");
  update(p,150);check(p.frame()==1,"Playback can continue from the new caller epoch");
  p.update(200,false,false,false,false,false);
  check(!p.active()&&!p.waiting()&&p.frame()==0,"Leaving seated posture cancels safely");
  update(p,100000);check(!p.active(),"Returning to seated does not resume cancelled work");
  check(!p.request(-1,true),"Negative uptime cannot create work");
  p.update(-1,true,false,false,false,true);check(!p.active(),"Negative updates cannot create automatic work");
 }
 public static void main(String[] args){
  directionAndFinish();noRestart();pauseAndResume();queuedManualAndCancel();automaticCadence();clockAndPosture();
  System.out.println("PASS "+checks+" seated eight-frame leg timeline, direction, pause, cadence and cancellation assertions");
 }
}
