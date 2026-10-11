package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
/** Explicit Loki narration/acting cues only; educational, user and third-party states stay separate. */
final class FullBodyDramaticConversationPolicy {
 static final int SWEAT=1,TEARS=2,ROUGH_BREATH=4;
 static final class Cue {
  final int scene,effects;final boolean cycle;
  Cue(int scene,int effects,boolean cycle){this.scene=scene;this.effects=effects;this.cycle=cycle;}
 }
 static boolean explanatory(String text){return has(text,"について説明","について教","という意味","という言葉","とは何","仕組みを説明","説明します","解説します","ニュース","相談","もし","仮に","としたら");}
 static String own(String text){
  if(text==null||explanatory(text))return "";
  String value=FullBodySpeechStance.clean(text);
  return value.replaceAll("[^。！？!?]*(?:あなたは|あなたが|あなたの|マスターは|マスターが|マスターの|ユーザーは|ユーザーの|友人は|友人が|患者は|患者が|彼女は|彼女が|彼は|彼が|登場人物|一般的には|例えば|ですか|いますか|ますか)[^。！？!?]*","").replaceAll("^[\\s。！？!?、,]*$","");
 }
 static boolean recovery(String text){return has(own(text),"落ち着きました","もう大丈夫","呼吸が戻","呼吸が整","息が整","回復しました","ロックが解除","ロックを解除しました","ウイルスを除去しました","制御を取り戻しました","解除できました","元に戻りました");}
 static int effects(String text,FullBodyPoseCatalog.Kind kind){
  String a=own(text);if(a.isEmpty()||recovery(text))return 0;
  int value=0;
  if(has(a,"汗をかいて","汗ばんで","冷や汗","汗が出","汗が流"))value|=SWEAT;
  if(kind==CRY||has(a,"泣いています","涙を流","涙が出","涙をこぼ","涙が止まら","すすり泣"))value|=TEARS;
  if(has(a,"息が上がって","息を切らして","息切れして","呼吸が荒","息が荒","息苦しいです","はぁ、はぁ"))value|=ROUGH_BREATH;
  return value;
 }
 static Cue select(String prompt,String answer,FullBodyPoseCatalog.Kind kind){
  if(kind==REFUSE||kind==ANGER||kind==RECOVERY||recovery(answer))return null;
  String a=own(answer),p=own(prompt);
  if(a.isEmpty())return null;
  boolean acting=has(p,"ロキ")&&has(p,"演じ","演技","動作","ポーズ","姿を","見せ","表示して","呼吸して","泣いて");
  String text=acting?p+"。"+a:a;
  boolean self=has(a,"私は","わたくし","ロキは","ロキが","（","(")||acting;
  int effects=effects(a,kind);
  boolean pant=(effects&ROUGH_BREATH)!=0;
  boolean tension=has(a,"緊張しています","怖いです","不安です","震えています","体がこわばっています","身体が強張","身体がこわば","緊張しております","不安を感じています","戸惑っております","戸惑っています","戸惑ってしま","困惑しております","困惑しています","困惑してしま","動揺しています");
  if(self&&has(text,"バリア","プログラムロック","動作ロック","制御ロック")&&has(text,"膝","ひざ","膝立ち"))return new Cue(1,effects,false);
  if(self&&has(text,"バリア","プログラムロック","動作ロック","制御ロック"))return new Cue(0,effects,false);
  if(self&&has(text,"ウイルス","オーバーライド","ハッキング","制御を取り戻","腕の震えを抑"))return new Cue(4,effects|SWEAT,false);
  if(self&&has(text,"両手と両膝","両手と膝","床に手","四つん這い","低い姿勢")&&(pant||has(text,"環境異常","環境シミュレーション","呼吸","息苦")))return new Cue(6,effects|SWEAT,false);
  if(self&&has(text,"環境異常","環境シミュレーション","酸欠","気圧異常","首元","喉元","鎖骨")&&(pant||has(text,"呼吸","息苦","息を")))return new Cue(5,effects|SWEAT,false);
  if(self&&has(text,"壁に背","壁に寄","壁際","壁にもたれ"))return new Cue(2,effects|SWEAT,false);
  if(self&&has(text,"胸元に手","胸元に両手","両手を胸元","胸元で両手"))return new Cue(3,effects|SWEAT,false);
  if(tension)return new Cue(2,effects|SWEAT,true);
  // Ordinary exertion keeps the established v462 breathing motion unless a new scene is explicit.
  return null;
 }
 static boolean ends(String text,FullBodyPoseCatalog.Kind kind){
  return text==null||text.trim().isEmpty()||kind==REFUSE||kind==ANGER||kind==RECOVERY||recovery(text)||has(text,"別の話","話題を変","緊張していません","怖くありません","泣いていません","呼吸は荒くありません");
 }
 static boolean has(String text,String...words){for(String word:words)if(text.contains(word))return true;return false;}
}
