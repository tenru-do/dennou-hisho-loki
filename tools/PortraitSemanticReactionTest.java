package com.example.rokidkeyboardbridge;
public final class PortraitSemanticReactionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  String answer=FullBodySemanticReaction.decode("おやめください。\n[[LOKI_REACTION:JOY:1:SMILE]]");FullBodySpeechStance.Cue cue=PortraitSemanticReaction.resolve("手を置く",answer);check(PortraitSemanticReaction.expression(cue)==55,"wrong joy corrected to existing refusal portrait");
  for(int i=1;i<=3;i++){answer=FullBodySemanticReaction.decode("思わず声が出ました。\n[[LOKI_REACTION:SURPRISE:"+i+":STARTLE]]");cue=PortraitSemanticReaction.resolve("大きな音",answer);check(PortraitSemanticReaction.expression(cue)==43+i,"three surprise levels");}
  answer=FullBodySemanticReaction.decode("少し距離を取らせてください。\n[[LOKI_REACTION:REFUSE:2:PROTECT]]");check(PortraitSemanticReaction.expression(PortraitSemanticReaction.resolve("",answer))==55,"meaning without fixed phrase");
  check(PortraitSemanticReaction.resolve("","普通のお知らせです。")==null,"missing cue retains legacy fallback");
  check(PortraitSemanticReaction.resolve("彼女が驚いたという話","彼女は驚いています。")==null,"third party not own emotion");
  for(FullBodyPoseCatalog.Kind k:new FullBodyPoseCatalog.Kind[]{FullBodyPoseCatalog.Kind.NEUTRAL,FullBodyPoseCatalog.Kind.JOY,FullBodyPoseCatalog.Kind.THINK,FullBodyPoseCatalog.Kind.ANGER,FullBodyPoseCatalog.Kind.SAD,FullBodyPoseCatalog.Kind.SHY,FullBodyPoseCatalog.Kind.WORRY,FullBodyPoseCatalog.Kind.RECOVERY,FullBodyPoseCatalog.Kind.STRAIN}){int id=PortraitSemanticReaction.expression(new FullBodySpeechStance.Cue(k,2));check(id>=0&&id<=61,"registered portrait "+k);}
  System.out.println("PASS portrait registry mapping, three surprise strengths, semantic-only refusal, correction of model joy, fallback and third-party exclusions");
 }
}
