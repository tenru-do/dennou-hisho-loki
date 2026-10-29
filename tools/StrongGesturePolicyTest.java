package com.example.rokidkeyboardbridge;
public final class StrongGesturePolicyTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  for(int strong=4;strong<=6;strong++){
   int base=KissGesturePolicy.base(strong);ok(base==strong-3,"stable base mapping");ok(KissGesturePolicy.approach(strong)>KissGesturePolicy.approach(base)*1.8f,"visibly stronger approach");
   boolean moves=false;for(long t=0;t<6000;t+=20){float sway=KissGesturePolicy.sway(strong,t),gain=KissGesturePolicy.envelope(strong,t);ok(Float.isFinite(sway)&&Math.abs(sway)<=1.001,"bounded sway");ok(gain>=0&&gain<=1.001,"bounded gain");ok(KissGesturePolicy.faceFrame(strong,t)<2&&KissGesturePolicy.bodyFrame(strong,t)<3,"existing assets only");moves|=Math.abs(sway)>.5f;}
   ok(moves,"additional oscillation reachable");ok(KissGesturePolicy.duration(strong)<4600,"one bounded clip");ok(KissGesturePolicy.sway(strong,6000)==0,"settles after clip");
  }
  String[] requests={"ディープキスを強めにして","突然のキスを激しくして","めろめろのキス、動きを大きくして"};FullBodyAffectionCue cue=new FullBodyAffectionCue();
  for(int i=0;i<3;i++){cue.user(requests[i],1000);cue.answer("そっとキスします",FullBodyPoseCatalog.Kind.SHY,1200);ok(cue.clip(1300,false)==16+i*2&&cue.clip(1300,true)==17+i*2,"strong variant retains posture");cue.user("やめて",1400);ok(cue.clip(1500,true)==-1,"stop overrides intensity");}
  cue.user("ディープキスして",1000);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1200);cue.user("もっと激しく",1400);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1500);ok(cue.clip(1600,true)==17,"intensity followup retains style");cue.user("控えめにして",1700);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1800);ok(cue.clip(1900,true)==11,"normal version retained");
  cue.user("突然キスして",1000);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1200);cue.user("激しい雨の予報",1400);cue.answer("雨の予報です",FullBodyPoseCatalog.Kind.NEUTRAL,1500);ok(cue.clip(1600,true)==-1,"ordinary intensity wording excluded");
  System.out.println("PASS "+n+" stronger motion/bounds/style/posture/stop assertions");
 }
}
