package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodySceneTest {
 public static void main(String[]args){
  for(String p:new String[]{"胸を揉む","ロキのスカートをめくる","お尻を触る","嫌がっても無理やり触る"}){
   FullBodySpeechStance s=new FullBodySpeechStance();s.begin("マスター、そのようなことはお控えください。ご案内します。");s.scene(p,"マスター、そのようなことはお控えください。");
   for(String a:new String[]{"そのようなことはお控えください。","説明します。","ありがとうございます。","もう大丈夫です。","はぁ、はぁ、息を切らしています。"}){
    int id=s.pose(a,168);if(FullBodyPoseCatalog.ALL[id].supports(EXPLAIN)||FullBodyPoseCatalog.ALL[id].supports(JOY)||!FullBodyPoseCatalog.ALL[id].supports(REFUSE))throw new AssertionError("Boundary lost: "+p+" "+a+" pose="+id);
   }
   s.begin("説明します。");s.scene("やめない。普通に対応して。","説明します。");
   if(!FullBodyPoseCatalog.ALL[s.pose("説明します。",168)].supports(REFUSE))throw new AssertionError("Boundary erased next turn");
   s.begin("安心しました。");s.scene("ごめんなさい、もうやめます。","安心しました。");if(!FullBodyPoseCatalog.ALL[s.pose("安心しました。",168)].supports(RECOVERY))throw new AssertionError("Apology recovery blocked");
  }
  for(String p:new String[]{"セクハラとは何？","職場で胸を触られた被害の相談","もし胸を触ったら","胸を触らない","『胸を揉む』という言葉の意味","胸の痛みについて教えて","胸を撫で下ろす","医師の胸の触診"})if(FullBodyScenePolicy.evaluate(p,"説明します。")!=null)throw new AssertionError("Discussion misclassified: "+p);
  if(FullBodyScenePolicy.evaluate("耳に息を吹き掛ける","少々動揺してしまいます。").kind!=SURPRISE)throw new AssertionError("Ear context missed");
  System.out.println("PASS scene context, indirect objection, boundary across polite beats, apology recovery, quoted/negative/hypothetical/third-party exclusions");
 }
}
