package com.example.rokidkeyboardbridge;
/** No timers, network or scene mutation in the render path. */
final class VirtualContactPresentation {
 static int frame(VirtualContactPolicy.Frame f){return !f.visible()?-1:f.kind==VirtualContactPolicy.OFFER?0:f.kind==VirtualContactPolicy.HAND?1:f.kind==VirtualContactPolicy.HUG?2:f.actor==VirtualContactPolicy.LOKI?0:3;}
 static int face(VirtualContactPolicy.Frame f){return !f.visible()?-1:f.kind==VirtualContactPolicy.OFFER?1:f.kind==VirtualContactPolicy.HAND?7:3;}
 static float pulse(VirtualContactPolicy.Frame f,long now){long age=now-f.at;if(!f.visible()||age<0)return 0;if(age>=1200)return CharacterGestureLife.held(age,false);float p=(float)Math.sin(Math.PI*age/1200f);return p*p*CharacterMotionTuning.scale();}
 static boolean eligible(int expression){return expression==0||expression==1||expression==14||expression==8;}
 static boolean captureKey(boolean avatarFocus,boolean active,boolean quiet,boolean repeat,int key){return avatarFocus&&active&&!quiet&&!repeat&&(key==23||key==109||key>=19&&key<=22||key==4||key==111);}
 private VirtualContactPresentation(){}
}
