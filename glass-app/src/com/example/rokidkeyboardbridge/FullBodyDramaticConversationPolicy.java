package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
/** Explicit Loki narration/acting cues only; educational, user and third-party states stay separate. */
final class FullBodyDramaticConversationPolicy {
 static final int SWEAT=1,TEARS=2,ROUGH_BREATH=4,CYBER_HIGHLIGHT=8;
 static final class Cue {
  final int scene,effects;final boolean cycle;
  Cue(int scene,int effects,boolean cycle){this.scene=scene;this.effects=effects;this.cycle=cycle;}
 }
 static boolean explanatory(String text){return has(text,"について説明","について教","という意味","という言葉","とは何","仕組みを説明","説明します","解説します","ニュース","相談","もし","仮に","としたら");}
 static String own(String text){
  if(text==null||explanatory(text))return "";
  String value=FullBodySpeechStance.clean(text).replaceAll("[^。！？!?]*(?:めまい|目眩|吐き気|悪寒|寒気|重圧|風圧|エラー)(?:は|が)(?:ありません|ない)[^。！？!?]*", "");
  return value.replaceAll("[^。！？!?]*(?:あなたは|あなたが|あなたの|マスターは|マスターが|マスターの|ユーザーは|ユーザーの|友人は|友人が|患者は|患者が|彼女は|彼女が|彼は|彼が|登場人物|一般的には|例えば|ですか|いますか|ますか)[^。！？!?]*","").replaceAll("^[\\s。！？!?、,]*$","");
 }
 static boolean recovery(String text){return has(own(text),"落ち着きました","もう大丈夫","呼吸が戻","呼吸が整","息が整","回復しました","ロックが解除","ロックを解除しました","ウイルスを除去しました","制御を取り戻しました","解除できました","元に戻りました","再起動が完了","負荷が下がりました","エラーを解消しました","警告を閉じました","重力が戻りました","外部コマンドを遮断しました","ふらつきが治まりました","吐き気が治まりました","寒気が治まりました");}
 static int effects(String text,FullBodyPoseCatalog.Kind kind){
  String a=own(text);if(a.isEmpty()||recovery(text))return 0;
  int value=0;
  if(has(a,"汗をかいて","汗ば","冷や汗","汗が出","汗が流","汗がにじ","汗が滲"))value|=SWEAT;
  if(kind==CRY||has(a,"泣いています","涙を流","涙が出","涙をこぼ","涙が止まら","すすり泣"))value|=TEARS;
  if(has(a,"息が上がって","息を切らして","息切れして","呼吸が荒","息が荒","息苦しいです","はぁ、はぁ"))value|=ROUGH_BREATH;
  return value;
 }
 static int postureRequest(String prompt){
  String p=FullBodyScenePolicy.clean(prompt);if(explanatory(p)||has(p,"彼女","友人","患者","登場人物","しゃがまない","しゃがむな","しゃがんでいない","ならない","という指示","と言った"))return -1;
  if(p.matches("[\\s\\S]*(?:しゃがんで(?:ください)?|しゃがめ|しゃがませて|かがんで(?:ください)?|屈んで(?:ください)?|腰を落として(?:ください)?)[。！!\\s]*$"))return 9;
  if(p.matches("[\\s\\S]*(?:立って(?:ください)?|立ち上がって(?:ください)?|立ち上がれ)[。！!\\s]*$"))return -2;
  return -1;
 }
 static Cue select(String prompt,String answer,FullBodyPoseCatalog.Kind kind){
  if(kind==REFUSE||kind==ANGER||kind==RECOVERY||recovery(answer))return null;
  FullBodySpeechStance.Cue guarded=FullBodyScenePolicy.evaluate(prompt,answer);
  if(guarded!=null&&guarded.kind==REFUSE)return null;
  if(postureRequest(prompt)==9&&!ends(answer,kind))return new Cue(9,0,false);
  String a=own(answer),p=own(prompt);
  if(a.isEmpty())return null;
  boolean acting=has(p,"ロキ")&&has(p,"演じ","演技","動作","ポーズ","姿を","見せ","表示して","呼吸して","泣いて");
  String text=acting?p+"。"+a:a;
  boolean self=has(a,"私は","わたくし","ロキは","ロキが","（","(")||acting;
  int effects=effects(a,kind);
  boolean pant=(effects&ROUGH_BREATH)!=0;
  boolean tension=has(a,"緊張しています","怖いです","不安です","震えています","体がこわばっています","身体が強張","身体がこわば","緊張しております","不安を感じています","戸惑っております","戸惑っています","戸惑ってしま","困惑しております","困惑しています","困惑してしま","動揺しています");
  FullBodySemanticReaction.Reaction semantic=FullBodySemanticReaction.find(answer);
  FullBodyConversationScenes.Rule tagged=semantic==null?null:FullBodyConversationScenes.action(semantic.action);
  // Metadata describes Loki's own actual reaction; guarded prose always wins.
  if(tagged!=null&&!explanatory(prompt)&&!ends(answer,kind)
      &&(self||semantic.cue.kind==WORRY||semantic.cue.kind==STRAIN||semantic.cue.kind==TIRED||semantic.cue.kind==SURPRISE||semantic.cue.kind==PANT||semantic.cue.kind==BREATH||semantic.cue.kind==CRY))
      return new Cue(tagged.scene,effects|tagged.effects(),false);
  if(self)for(FullBodyConversationScenes.Rule rule:FullBodyConversationScenes.ALL)
      if(rule.matches(text))return new Cue(rule.scene,effects|rule.effects(),false);
  if(tension)return new Cue(2,effects|SWEAT,true);
  // Ordinary exertion keeps the established v462 breathing motion unless a new scene is explicit.
  return null;
 }
 static boolean ends(String text,FullBodyPoseCatalog.Kind kind){
  return text==null||text.trim().isEmpty()||text.matches("[\\s\\S]*(?:めまい|目眩|吐き気|悪寒|寒気|重圧|風圧|エラー)(?:は|が)(?:ありません|ない)[\\s\\S]*")||kind==REFUSE||kind==ANGER||kind==RECOVERY||recovery(text)||has(text,"別の話","話題を変","緊張していません","怖くありません","泣いていません","呼吸は荒くありません");
 }
 static boolean has(String text,String...words){for(String word:words)if(text.contains(word))return true;return false;}
}
