package com.example.rokidkeyboardbridge;
public final class KissGesturePolicyTest {
 static int checks;static void ok(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 public static void main(String[]args){
  java.util.Set<Long> durations=new java.util.HashSet<>();
  for(int type=0;type<4;type++){durations.add(KissGesturePolicy.duration(type));for(long t=-100;t<6000;t+=20){int f=KissGesturePolicy.faceFrame(type,t),b=KissGesturePolicy.bodyFrame(type,t);ok(f>=0&&f<2&&b>=0&&b<3,"frame bounds");float e=KissGesturePolicy.envelope(type,t);ok(Float.isFinite(e)&&e>=0&&e<=1.001,"bounded approach");}ok(KissGesturePolicy.bodyFrame(type,6000)==0,"settles after gesture");}
  ok(durations.size()==4,"four visibly distinct rhythms");
  FullBodyAffectionCue cue=new FullBodyAffectionCue();String[] names={"フレンチキスして","ディープキスして","突然キスして","めろめろでキスして"};
  for(int type=0;type<4;type++){cue.user(names[type],1000);cue.answer("そっとキスします",FullBodyPoseCatalog.Kind.SHY,1200);ok(cue.clip(1300,false)==8+type*2&&cue.clip(1300,true)==9+type*2,"type and posture retained");cue.user("やめて",1400);ok(cue.clip(1500,true)==-1,"stop kiss");}
  cue.user("キスして",1000);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1200);cue.user("ディープにして",1400);cue.answer("ゆっくりキスします",FullBodyPoseCatalog.Kind.SHY,1500);ok(cue.clip(1600,true)==11,"contextual type followup");
  ok(FullBodyAffectionCue.request("フレンチ料理の店を探して")==-1,"food is not a kiss");ok(FullBodyAffectionCue.request("キスとは何ですか")==-1,"definition is not a kiss");
  cue.user("突然キスして",1000);cue.answer("驚かせるようにキスします",FullBodyPoseCatalog.Kind.SURPRISE,1200);ok(cue.clip(1300,true)==13,"positive surprise style retained");
  cue.user("甘えて",1000);cue.answer("驚きました",FullBodyPoseCatalog.Kind.SURPRISE,1200);ok(cue.clip(1300,true)==-1,"unrelated surprise preempts affection");
  for(String ordinary:new String[]{"もっとフレンチ料理の店を探して","ディープとは何ですか","キス釣りの道具は何が必要"}){cue.user("キスして",1000);cue.answer("キスします",FullBodyPoseCatalog.Kind.SHY,1200);cue.user(ordinary,1400);cue.answer("説明します",FullBodyPoseCatalog.Kind.NEUTRAL,1500);ok(cue.clip(1600,true)==-1,"ordinary informational followup clears kiss");}
  cue.user("キスして",1000);cue.answer("キスしませんか",FullBodyPoseCatalog.Kind.CONFIDENT,1200);ok(cue.clip(1300,true)==9,"positive invitation wording");cue.answer("キスはしません",FullBodyPoseCatalog.Kind.REFUSE,1400);ok(cue.clip(1500,true)==-1,"actual refusal wording");
  ok(FullBodyAffectionCue.stop("キスはしませんから安心して"),"explanatory refusal is not an invitation");ok(!FullBodyAffectionCue.stop("キスしませんか？"),"question invitation wording");
  System.out.println("PASS "+checks+" four kissing rhythms/type/posture/stop/expiry assertions");
 }
}
