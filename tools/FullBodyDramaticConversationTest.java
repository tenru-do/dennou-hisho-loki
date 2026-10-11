package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyDramaticConversationTest {
 static int checks;
 static void scene(String prompt,String text,FullBodyPoseCatalog.Kind kind,int expected,boolean cycle){
  FullBodyDramaticConversationPolicy.Cue c=FullBodyDramaticConversationPolicy.select(prompt,text,kind);
  if(expected<0?c!=null:c==null||c.scene!=expected||c.cycle!=cycle)throw new AssertionError(text+" -> "+(c==null?"none":c.scene));
  checks++;
 }
 static void check(boolean v,String label){if(!v)throw new AssertionError(label);checks++;}
 public static void main(String[] args){
  scene("","私はバリアで動作が止まっています。",STRAIN,0,false);
  scene("","私はプログラムロックで膝をついています。",STRAIN,1,false);
  scene("","私は壁に背を預けています。冷や汗が出ています。",WORRY,2,false);
  scene("","私は胸元に両手を当てています。呼吸が荒くなっています。",PANT,3,false);
  scene("","私はウイルスによる腕の震えを抑えています。",WORRY,4,false);
  scene("","私は環境異常で息苦しいです。",PANT,5,false);
  scene("","私は床に手をついて低い姿勢で呼吸しています。",PANT,6,false);
  scene("","少し緊張しています。",WORRY,2,true);
  scene("ロキ、壁際で動揺するポーズを見せて。","この姿をお見せします。",NEUTRAL,2,false);
  scene("","息が上がってしまいました。",PANT,-1,false);
  scene("","あなたは緊張していますね。",WORRY,-1,false);
  scene("","あなたの呼吸が荒く、緊張していますね。",WORRY,-1,false);
  scene("","彼女は壁に背を預けています。",WORRY,-1,false);
  scene("","友人が緊張しています。",WORRY,-1,false);
  scene("","緊張していますか？",WORRY,-1,false);
  scene("","もし私はバリアで止まったとしたら。",WORRY,-1,false);
  scene("","私はプログラムロックについて説明します。",EXPLAIN,-1,false);
  scene("","私はハッキングの仕組みを説明します。",EXPLAIN,-1,false);
  scene("","私は緊張していません。",NEUTRAL,-1,false);
  scene("","私は緊張しています。",REFUSE,-1,false);
  scene("","私は壁に寄っています。",ANGER,-1,false);
  scene("","私はもう大丈夫です。呼吸が整いました。",RECOVERY,-1,false);
  scene("","私はロックが解除できました。",NEUTRAL,-1,false);
  scene("","私は制御を取り戻しました。",CONFIDENT,-1,false);
  scene("","私はロックを解除しました。",NEUTRAL,-1,false);
  check(FullBodyDramaticConversationPolicy.effects("私は冷や汗が出ています。涙を流しています。呼吸が荒いです。",CRY)==7,"combined face/breath cues");
  check(FullBodyDramaticConversationPolicy.effects("あなたが涙を流しています。",CRY)==0,"user tears");
  check(FullBodyDramaticConversationPolicy.effects("涙について説明します。",CRY)==0,"educational tears");
  check(FullBodyDramaticConversationPolicy.effects("私は泣いていません。",CRY)==0,"negative tears");
  check(FullBodyDramaticConversationPolicy.ends("もう大丈夫です。",NEUTRAL),"recovery ends");
  check(FullBodyDramaticConversationPolicy.ends("別の話をしましょう。",NEUTRAL),"topic change ends");
  check(FullBodyDramaticConversationPolicy.ends("",NEUTRAL),"empty ends");
  check(FullBodyDramaticConversationPolicy.ends("私は怖くありません。",NEUTRAL),"negated fear ends");
  check(!FullBodyDramaticConversationPolicy.ends("あなたが緊張しています。",WORRY),"neutral/user sentence alone keeps held reaction");
  System.out.println("PASS dramatic conversation: "+checks+" scene, identity, explanation, refusal, recovery and effect checks");
 }
}
