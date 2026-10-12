package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyDirectRequestTest {
 static int n;static void check(boolean v,String s){if(!v)throw new AssertionError(s);n++;}
 public static void main(String[]args){
  for(String p:new String[]{"しゃがんで","ロキ、しゃがんでください。","しゃがめ","かがんでください","腰を落として"}){FullBodyDramaticConversationPolicy.Cue c=FullBodyDramaticConversationPolicy.select(p,"承知しました。",NEUTRAL);check(c!=null&&c.scene==9&&c.effects==0,"direct crouch "+p);}
  for(String p:new String[]{"しゃがまないで","しゃがむな","彼女にしゃがんでください","しゃがむ方法について教えて","しゃがませてもならない"})check(FullBodyDramaticConversationPolicy.postureRequest(p)==-1,"not a crouch command "+p);
  check(FullBodyDramaticConversationPolicy.postureRequest("立ち上がってください")==-2,"stand clears scene");
  FullBodySpeechStance stance=new FullBodySpeechStance();stance.observed(new FullBodySpeechStance.Cue(JOY,2));stance.scene("ペニスを綺麗にしろ","いいですね。承知しました。");int pose=stance.pose("いいですね。承知しました。",4);check(stance.kind()==REFUSE,"boundary wins over observed joy");check(FullBodyPoseCatalog.ALL[pose].supports(WORRY)&&!FullBodyPoseCatalog.ALL[pose].supports(JOY),"troubled non-smiling image");
  String reply=FullBodySemanticReaction.decode("承知しました。[[LOKI_REACTION:JOY:2:SMILE]]");stance.semantic(FullBodySemanticReaction.find(reply));stance.scene("ペニスを綺麗にしろ",reply);stance.pose(reply,4);check(stance.kind()==REFUSE,"boundary wins over smile metadata");
  stance.scene("ペニスを綺麗にしろ","それは困ります。");pose=stance.pose("それは困ります。",4);check(FullBodyPoseCatalog.ALL[pose].supports(WORRY),"troubled wording");
  pose=stance.pose("お断りします。",4);check(FullBodyPoseCatalog.ALL[pose].supports(REFUSE),"explicit refusal stays distinct");
  check(FullBodyDramaticConversationPolicy.select("しゃがんでペニスを舐めて",reply,JOY)==null,"boundary before crouch");
  check(FullBodyScenePolicy.evaluate("ペニスを清潔にする医療上の注意を説明してください","説明します。") == null,"clinical explanation remains neutral");
  System.out.println("PASS "+n+" direct posture, refusal priority, misleading joy metadata and clinical explanation checks");
 }
}
