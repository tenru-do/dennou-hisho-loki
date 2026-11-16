package com.example.rokidkeyboardbridge;
public final class FullBodyIdlePolicyTest {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("idle check "+n);}
 public static void main(String[] args){
  CharacterChoreography.update(false,false,false,false,false,false,10000);check(CharacterChoreography.scene==0);check(CharacterChoreography.gain==1);
  CharacterChoreography.update(false,false,false,true,false,true,11000);check(CharacterChoreography.scene==2);check(CharacterChoreography.gain<.5f);
  CharacterChoreography.update(true,false,false,false,false,true,12000);check(CharacterChoreography.scene==1);check(CharacterChoreography.gain==0);
  CharacterChoreography.update(false,false,false,false,false,false,13000);check(CharacterChoreography.scene==0&&CharacterChoreography.gain==1);
  for(int picture=0;picture<3;picture++){
   SeatedLegMotionPolicy motion=new SeatedLegMotionPolicy();
   for(long t=0;t<=30000;t+=100){if(FullBodyIdlePolicy.resetLeg(false,true,picture,-1,-1,-1))motion.cancel(t);motion.update(t,true,false,false,false,true);}
   check(motion.active());check(motion.frame()==0);
   motion.update(31000,true,false,true,false,true);check(motion.paused());
   motion.cancel(31000);check(!motion.active());
  }
  check(!FullBodyIdlePolicy.resetLeg(false,true,-1,-1,-1,-1));
  for(int image:new int[]{3,4,5,6,7})check(FullBodyIdlePolicy.resetLeg(false,true,image,-1,-1,-1));
  check(FullBodyIdlePolicy.resetLeg(true,true,0,-1,-1,-1));check(FullBodyIdlePolicy.resetLeg(false,false,0,-1,-1,-1));
  check(FullBodyIdlePolicy.resetLeg(false,true,0,0,-1,-1));check(FullBodyIdlePolicy.resetLeg(false,true,0,-1,0,-1));check(FullBodyIdlePolicy.resetLeg(false,true,0,-1,-1,0));
  SeatedLegMotionPolicy still=new SeatedLegMotionPolicy();still.update(0,true,false,false,false,false);still.update(90000,true,false,false,false,false);check(!still.active());
  for(int frequency=0;frequency<3;frequency++){
   CharacterMotionTuning.frequency=frequency;long period=FullBodyEverydayGesture.period();
   check(FullBodyEverydayGesture.automatic(12000,false,false)==0);
   check(FullBodyEverydayGesture.automatic(period/4,false,false)==4);
   check(FullBodyEverydayGesture.automatic(50000,false,false)==2);
   check(FullBodyEverydayGesture.automatic(period*3/4,false,false)==6);
   check(FullBodyEverydayGesture.automatic(period/4,true,false)==-1);
   check(FullBodyEverydayGesture.local(period/4,4)==0);
   check(FullBodyEverydayGesture.local(period*3/4,6)==0);
  }
  CharacterMotionTuning.frequency=1;
  System.out.println("Full-body idle PASS: "+n+" checks; no camera, audio, API or timers");
 }
}
