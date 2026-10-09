package com.example.rokidkeyboardbridge;
public final class FullBodySemanticReactionTest {
 static void check(boolean b,String t){if(!b)throw new AssertionError(t);}
 public static void main(String[]args){
  check(FullBodySpeechStance.classify("どうか、おやめください。").kind==FullBodyPoseCatalog.Kind.REFUSE,"polite objection");
  check(FullBodySpeechStance.classify("息を呑んでしまいました。").kind==FullBodyPoseCatalog.Kind.SURPRISE,"surprise inflection");
  check(FullBodySpeechStance.classify("身体は硬直し、言葉が出ません。").kind==FullBodyPoseCatalog.Kind.STRAIN,"physical tension");
  check(FullBodySpeechStance.classify("彼女は息を呑んでしまいました。")==null,"third party excluded");
  check(FullBodySpeechStance.classify("驚いていません。")==null,"negated feeling excluded");
  String text=FullBodySemanticReaction.decode("少し距離を取らせてください。\n[[LOKI_REACTION:REFUSE:2:PROTECT]]");
  check(!text.contains("LOKI_REACTION")&&FullBodySemanticReaction.find(text).cue.kind==FullBodyPoseCatalog.Kind.REFUSE,"metadata removed and retained internally");
  FullBodySpeechStance stance=new FullBodySpeechStance();stance.begin(text);stance.semantic(FullBodySemanticReaction.find(text));stance.scene("近づいてきた",text);
  int pose=stance.pose(text,168);check(FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.REFUSE)&&FullBodyMotionPolicy.isSeated(pose)&&FullBodyPoseCatalog.ALL[pose].gesture.contains("protective"),"semantic protective seated pose");
  String wrong=FullBodySemanticReaction.decode("おやめください。\n[[LOKI_REACTION:JOY:1:SMILE]]");stance.begin(wrong);stance.semantic(FullBodySemanticReaction.find(wrong));stance.scene("手を置く",wrong);pose=stance.pose(wrong,4);
  check(FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.REFUSE),"automatic correction of joyful refusal");
  String surprise=FullBodySemanticReaction.decode("まったく予想していませんでした。\n[[LOKI_REACTION:SURPRISE:2:STARTLE]]");stance=new FullBodySpeechStance();stance.begin(surprise);stance.semantic(FullBodySemanticReaction.find(surprise));pose=stance.pose(surprise,4);check(FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.SURPRISE),"meaning beyond keyword fallback");
  check(FullBodySemanticReaction.decode("本文[[LOKI_REACTION:BAD:2:PROTECT]]続き").equals("本文続き"),"invalid metadata does not leak or remove text");
  check(FullBodySemanticReaction.decode("本文\n[[LOKI_REACTION:REF").equals("本文"),"truncated tag never spoken");
  check(FullBodySemanticReaction.decode("普通の回答です。").equals("普通の回答です。"),"plain response unchanged");
  System.out.println("PASS polite boundary, surprise/tension variants, third-party/negation, metadata parsing, semantic gesture/posture, automatic mismatch correction, missing/invalid/truncated fallback");
 }
}
