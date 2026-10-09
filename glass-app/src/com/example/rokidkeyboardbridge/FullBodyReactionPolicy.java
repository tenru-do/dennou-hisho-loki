package com.example.rokidkeyboardbridge;
/** Latest-intent coalescing: a short speech pause must not trigger sitting/standing. */
final class FullBodyReactionPolicy {
 private int pending=-1; private long pendingSince, committedAt; private int lastMode;
 int target(int expression,int mode,int current,long now,boolean transitioning){
  int desired=FullBodyMotionPolicy.poseForState(expression,mode);
  boolean seated=FullBodyMotionPolicy.isSeated(current);
  if(mode==1 && seated)desired=168; // Listen in the current seated posture.
  if(mode==2 && seated && (expression==0||expression==14))desired=168;
  return targetPose(desired,mode,current,now,transitioning);
 }
 int targetPose(int desired,int mode,int current,long now,boolean transitioning){
  desired=Math.max(0,Math.min(180,desired));
  if(mode!=lastMode){if(mode==0)pendingSince=now;lastMode=mode;}
  if(desired!=pending){pending=desired;pendingSince=now;}
  if(current<0)return desired;
  if(desired==current)return current;
  long settle=mode==0&&desired==168?1800:250;
  if(transitioning || now-pendingSince<settle || now-committedAt<900)return current;
  return desired;
 }
 void committed(long now){committedAt=now;}
 void reset(){pending=-1;pendingSince=committedAt=0;lastMode=0;}
}
