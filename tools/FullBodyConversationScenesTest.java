package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyConversationScenesTest {
 static int checks;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
 public static void main(String[]args){
  boolean[] covered=new boolean[FullBodyDramaticGallery.LABELS.length];
  for(FullBodyConversationScenes.Rule rule:FullBodyConversationScenes.ALL){
   check(!covered[rule.scene],"unique scene");covered[rule.scene]=true;
   String words="私は";for(String[] group:rule.words)words+=group[0]+" ";
   FullBodyDramaticConversationPolicy.Cue c=FullBodyDramaticConversationPolicy.select("",words+"状態です。",STRAIN);
   check(c!=null&&c.scene==rule.scene,"keyword "+rule.key);
   String text=FullBodySemanticReaction.decode("状態に合わせて動いています。\n[[LOKI_REACTION:STRAIN:2:SCENE_"+rule.key+"]]");
   check(!text.contains("LOKI_REACTION"),"not spoken");
   c=FullBodyDramaticConversationPolicy.select("",text,STRAIN);check(c!=null&&c.scene==rule.scene,"semantic without pronoun "+rule.key);
   check(FullBodyDramaticConversationPolicy.select("仕組みを説明してください",text,STRAIN)==null,"explanation prompt");
   check(FullBodyDramaticConversationPolicy.select("",text,REFUSE)==null,"refusal priority");
   text=FullBodySemanticReaction.decode("あなたはめまいでふらついています。\n[[LOKI_REACTION:STRAIN:2:SCENE_"+rule.key+"]]");
   check(FullBodyDramaticConversationPolicy.select("",text,STRAIN)==null,"third party text overrides metadata");
   text=FullBodySemanticReaction.decode("私はもう大丈夫です。\n[[LOKI_REACTION:STRAIN:2:SCENE_"+rule.key+"]]");
   check(FullBodyDramaticConversationPolicy.select("",text,STRAIN)==null,"recovery text overrides metadata");
  }
  for(boolean present:covered)check(present,"all registered scenes reachable");
  String text=FullBodySemanticReaction.decode("同じ返答。[[LOKI_REACTION:WORRY:2:SCENE_DIZZY]]");check(FullBodySemanticReaction.find(text)!=null,"record");
  FullBodySemanticReaction.decode("同じ返答。");check(FullBodySemanticReaction.find(text)==null,"untagged identical answer cannot inherit stale scene");
  text=FullBodySemanticReaction.decode("通常の返答。[[LOKI_REACTION:WORRY:2:SCENE_UNKNOWN]]");check(FullBodySemanticReaction.find(text)==null&&!text.contains("LOKI"),"unknown action removed");
  String[] aliases={"私は転びそうです。","私はくらくらしています。","私はみぞおちが不快です。"};int[] expected={11,14,15};
  for(int i=0;i<aliases.length;i++){FullBodyDramaticConversationPolicy.Cue c=FullBodyDramaticConversationPolicy.select("",aliases[i],STRAIN);check(c!=null&&c.scene==expected[i],"alias");}
  check(FullBodySemanticReaction.INSTRUCTION.contains("SCENE_DIZZY"),"instruction uses actual registry");
  System.out.println("PASS unified scenes "+checks+" keyword/semantic/priority/stale-cache checks");
 }
}
