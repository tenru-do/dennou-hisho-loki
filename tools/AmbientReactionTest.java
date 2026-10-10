package com.example.rokidkeyboardbridge;
public final class AmbientReactionTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  String source="今の言い間違いから、最後に最初の話へ戻すところが面白かったですね。あはは、あの間の取り方でつい笑ってしまいました。次にどう返すかも楽しみです。";
  String answer=AmbientScenePolicy.comment(source,"joy",2);check(answer.equals(source),"comment must remain complete and metadata invisible");
  FullBodySemanticReaction.Reaction reaction=FullBodySemanticReaction.find(answer);check(reaction!=null&&reaction.cue.kind==FullBodyPoseCatalog.Kind.JOY&&reaction.cue.intensity==2,"observed intensity retained");
  FullBodySpeechStance stance=new FullBodySpeechStance();stance.begin(answer);stance.semantic(reaction);
  for(String beat:new String[]{source,"楽しみですね。","どうなるでしょう。"}){int pose=stance.pose(beat,4);check(!FullBodyPoseCatalog.ALL[pose].seated&&FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.JOY),"standing joy must survive TTS beats");}
  String think=AmbientScenePolicy.comment("この仕組みは興味深いですね。前の説明との違いがどこにあるのか、もう少し考えてみたいです。","think",1);
  stance.observed(null);stance.begin(think);stance.semantic(FullBodySemanticReaction.find(think));int pose=stance.pose(think,4);check(stance.kind()==FullBodyPoseCatalog.Kind.THINK&&!stance.laughing(),"analysis must not become shy or laughter");
  FullBodyPoseVariation variation=new FullBodyPoseVariation();
  for(int base:new int[]{4,168}){variation.reset();for(long time=0;time<600000;time+=100){int id=variation.target(base,0,time,null);FullBodyPoseCatalog.Pose p=FullBodyPoseCatalog.ALL[id];check(!p.supports(FullBodyPoseCatalog.Kind.SHY)&&!p.supports(FullBodyPoseCatalog.Kind.REFUSE)&&!p.supports(FullBodyPoseCatalog.Kind.JOY),"unprompted shy/refusal/laughter during idle");check(p.seated==FullBodyMotionPolicy.isSeated(base),"posture continuity");}}
  for(long time=0;time<600000;time+=100)check(!FullBodyPoseCatalog.ALL[FullBodySeatedPolicy.waitingPose(time)].supports(FullBodyPoseCatalog.Kind.REFUSE),"waiting must not cross arms in refusal");
  check(AmbientScenePolicy.comment("", "joy",1).isEmpty(),"empty observation creates no speech");
  check(AmbientScenePolicy.comment("あ".repeat(300),"neutral",0).length()==220,"bounded extended comments");
  System.out.println("PASS complete AMB comments, emotion/intensity across speech beats, same posture and no unprompted shy/refusal/laughter over ten minutes");
 }
}