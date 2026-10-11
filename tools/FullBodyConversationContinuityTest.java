package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyConversationContinuityTest {
 static int checks;
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);checks++;}
 public static void main(String[]args){
  FullBodyConversationContinuity c=new FullBodyConversationContinuity();
  int worry=FullBodyPoseCatalog.select(WORRY,1,168,0);
  c.active(true,100);c.accept(worry,WORRY,1,false,100);
  for(long t:new long[]{101,1000,180100,600000})check(c.holds(t)&&c.pose()==worry,"active conversation keeps seated reaction");
  c.active(false,600000);
  check(c.holds(779999)&&!c.holds(780000),"three-minute interturn window");
  FullBodyConversationContinuity expired=new FullBodyConversationContinuity();expired.accept(worry,WORRY,1,false,0);expired.active(true,180000);check(!expired.holds(180000),"new session cannot revive expired reaction");
  check(c.accept(worry,WORRY,1,false,610000)==worry,"new answer retains reaction");
  int alternate=FullBodyPoseCatalog.select(WORRY,1,168,1);
  check(c.accept(alternate,WORRY,1,false,620000)==worry,"same emotion avoids random pose rotation");
  int happy=FullBodyPoseCatalog.select(JOY,1,168,0);
  check(c.accept(happy,JOY,1,false,620100)==happy,"new emotion replaces reaction");
  c.clear();check(!c.holds(620101),"explicit stop clears");
  check(FullBodyConversationContinuity.end("今日はここまで。"),"user end");
  check(FullBodyConversationContinuity.topic("別の話、天気を教えて。"),"topic");
  for(String answer:new String[]{"お隣に座らせていただきます。","お座りいたします。"}){
   FullBodySpeechStance s=new FullBodySpeechStance();s.begin(answer);int pose=s.pose(answer,4);
   check(pose>=0&&FullBodyMotionPolicy.isSeated(pose),"natural seated wording");
  }
  FullBodySpeechStance s=new FullBodySpeechStance();s.begin("私は不安です。");int pose=s.pose("私は不安です。",168);
  s.begin("承知しました。",true);check(s.pose("承知しました。",pose)>=0&&s.kind()==WORRY,"neutral acknowledgement carries emotion");
  s.begin("私は安心しました。",true);s.pose("私は安心しました。",pose);check(s.kind()==RECOVERY,"recovery replaces emotion");
  String[] examples={"私はバリアで停止しています。","私はプログラムロックで膝をついています。","私は壁に背を預けて不安です。","私は胸元に両手を当てています。呼吸が荒いです。","私はウイルスによる腕の震えを抑えています。","私は環境異常で息苦しいです。","私は床に手をついて低い姿勢で呼吸しています。"};
  for(int i=0;i<examples.length;i++){FullBodySpeechStance q=new FullBodySpeechStance();q.begin(examples[i]);q.pose(examples[i],4);FullBodyDramaticConversationPolicy.Cue cue=FullBodyDramaticConversationPolicy.select("",examples[i],q.kind());check(cue!=null&&cue.scene==i,"registered scene "+i);}
  check(FullBodyDramaticConversationPolicy.select("","私は少々困惑してしまいます。",WORRY).cycle,"natural wording starts stress cycle");
  check(FullBodyScenePolicy.evaluate("文章に文字を挿入して","承知しました。")==null,"ordinary insertion is not a boundary cue");
  check(FullBodyScenePolicy.evaluate("乳首の痛みの原因","確認します。")==null,"clinical question does not become a gesture");
  System.out.println("PASS continuity: "+checks+" active/listening/gap/timeout/end/recovery/posture/emotion/registered-scene checks");
 }
}
