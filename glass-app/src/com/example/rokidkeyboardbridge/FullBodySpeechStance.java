package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;

/** Select direct full-body IDs, with stance continuity across speech beats. */
final class FullBodySpeechStance {
 static final class Cue {
  final FullBodyPoseCatalog.Kind kind;final int intensity;
  Cue(FullBodyPoseCatalog.Kind k,int i){kind=k;intensity=i;}
 }
 private Cue scene;
 private Cue observed;
 private String semanticAction;
 void observed(Cue cue){observed=cue;semanticAction=null;if(cue!=null)scene=null;}
 void semantic(FullBodySemanticReaction.Reaction reaction){observed=reaction.cue;semanticAction=reaction.action;}
 void scene(String prompt,String answer){
  Cue next=FullBodyScenePolicy.evaluate(prompt,answer);
  boolean leave=FullBodyScenePolicy.has(FullBodyScenePolicy.clean(prompt),"ごめん","謝","すみません","やめます","もうやめる","もうやめた","TOPIC","ラジオ","ジークンドー","ペン","コーヒー","触診","診察","医師","医療","胸を撫で下ろ","胸をなでおろ","別の話","話題を変","天気","予定","ニュース","について教","とは","相談");
  if(next!=null||scene==null||scene.kind!=REFUSE||leave)scene=next;
 }
 private boolean laughing;
 private Cue active;private boolean refusing;private int variant;private String last="";
 private final int[] rotations=new int[FullBodyPoseCatalog.Kind.values().length];
 void begin(String answer){reset();}
 FullBodyPoseCatalog.Kind kind(){return active==null?null:active.kind;}
 boolean laughing(){return laughing && active!=null && active.kind==JOY;}
 static boolean laughCue(String text){return has(clean(text),"あはは","ははは","ふふっ","ふふ、","笑っちゃ","笑ってしま","思わず笑","笑っています","おかしくて笑","面白くて笑");}
 void reset(){laughing=false;active=null;refusing=false;last="";}
 int pose(String text,int current){
  String postureText=clean(text);
  if(has(postureText,"座って","椅子に座","座ったまま","腰掛け","腰かけ"))current=168;
  else if(has(postureText,"立ち上がる","立ち上がり","立ったまま"))current=4;
  Cue cue=classify(text);
  Cue wording=cue;
  if(observed!=null)cue=observed;
  // An explicit objection must never become a confident/smiling model gesture.
  if(wording!=null&&(wording.kind==REFUSE||wording.kind==ANGER))cue=wording;
  if(scene!=null){
   if(scene.kind==REFUSE && (cue==null||!(cue.kind==REFUSE||cue.kind==ANGER||cue.kind==SAD||cue.kind==CRY||cue.kind==WORRY||cue.kind==STRAIN||cue.kind==TIRED)))cue=scene;
   else if(scene.kind!=REFUSE && (cue==null || !(cue.kind==RECOVERY||cue.kind==REFUSE||cue.kind==ANGER)))cue=scene;
  }
  laughing=laughCue(text);
  if(cue!=null){
   if(cue.kind==RECOVERY){refusing=false;active=cue;}
   else if(cue.kind==REFUSE){refusing=true;active=cue;}
   else if(refusing&&(cue.kind==ANGER||cue.kind==SAD||cue.kind==CRY||cue.kind==WORRY||cue.kind==STRAIN||cue.kind==TIRED))active=cue;
   else if(!refusing)active=cue;
   if(!last.equals(text))variant=rotations[active.kind.ordinal()]++;
  }
  last=text==null?"":text;
  return active==null?-1:FullBodySemanticReaction.actionPose(FullBodyPoseCatalog.select(active.kind,active.intensity,current,variant),active,semanticAction,current);
 }
 static Cue classify(String text){
  String v=clean(text);if(v.isEmpty())return null;
  if(has(v,"という意味","という言葉","とは何","について説明","について教","と言っていました","と言われました")
       &&!has(v,"私は","ロキは","（","("))return null;
  if(v.equals("嫌です")||has(v,"おやめください","おやめ下さい","やめていただ","お控えください","お控えいただ","やめてください","やめて下さい","やめてほしい","やめて！","やめて。","触らないで","触れないで","触られるのは嫌",
       "嫌です。","嫌です！","嫌だよ","お断りします","応じられません","受け入れられません",
       "その発言は不快","そういう発言は不快","困ります","遠慮してください","失礼です","不適切です","控えてください","やめなさい","だめです","セクハラです","セクハラに当たります","セクハラになります","拒絶する","拒む","首を横に振る"))return cue(REFUSE,v,2);
  if(has(v,"落ち着きました","もう大丈夫です","ほっとしました","安心しました","息を整える","呼吸を整える","深呼吸する","息を整えます","落ち着いてきました"))return cue(RECOVERY,v,1);
  if(has(v,"怒っています","腹が立ち","怒ります","不快です","不快に感じ","許せません","拳を握る","拳を握り"))return cue(ANGER,v,2);
  if(has(v,"号泣する","号泣して","泣き叫ぶ","泣き叫ん","涙が止まら","すすり泣く","涙をこぼす","涙を流す","涙が出ます","泣いています","泣いてしま","むせび泣"))return cue(CRY,v,2);
  if(has(v,"怖いです","怖くな","恐ろしいです","不安です","心配です","心配ですね","つらかったですね","つらいですね","大変でしたね","無理しないで","お大事に","震えている","震える手","体をこわばらせ","申し訳ありません","ごめんなさい","すみません"))return cue(WORRY,v,1);
  if(has(v,"悲しいです","悲しくな","悲しんで","つらいです","残念です","寂しいです","傷つきました","落ち込んでいます","肩を落とす"))return cue(SAD,v,1);
  if(has(v,"息を呑ん","息をのん","息をのみ","言葉を失","びくっと","びっくりしました","びっくり！","びっくりした","びっくりしちゃ","びっくりです","びっくりさせ","驚いちゃ","驚いた","驚かせ","ドキッ","どきっ","ひゃっ","ひゃ！","きゃっ","わっ！","わっ、","ビクッ","びくっ","驚きました","驚いています","驚いて目を","目を見開く","息をのむ","息を呑む","えっ！","えっ？","まさか！"))return cue(SURPRISE,v,2);
  if(has(v,"本当ですか","えっ、","なんと！","動揺して","動揺しちゃ","動揺する","どきどき","ドキドキ"))return cue(SURPRISE,v,1);
  if(has(v,"喘いで","喘ぐ","喘ぎながら","喘ぎ声を漏らす","喘ぎ声が漏れ","息を切らす","息を切らして","息が上がって","息が上がっています","呼吸が荒く","呼吸が荒い","息が荒い","荒い息","はぁ、はぁ","はあ、はあ"))return cue(PANT,v,2);
  if(has(v,"吐息を漏らす","吐息が漏れ","息を弾ませ","胸が上下","息を吐く","ため息をつく","ため息が出","呼吸を深く"))return cue(BREATH,v,1);
  if(has(v,"疲れました","疲れています","疲れてしま","疲れたよ","眠いです","くたくたです","ぐったりする","ぐったりして","疲れ果て","力尽きて"))return cue(TIRED,v,2);
  if(has(v,"恥ずかしいです","恥ずかしくな","照れます","照れちゃ","頬を赤らめ","照れながら","はにかむ"))return cue(SHY,v,1);
  if(laughCue(text)||has(v,"うれしいです","嬉しいです","嬉しくな","嬉しくて","楽しいです","楽しくな","ありがとうございます","ありがとう！","よかったですね","よかったです","いいですね","素晴らしいですね","ふふ、","おめでとう","笑みを浮かべ","微笑む","にっこり","笑っています"))return cue(JOY,v,1);
  if(has(v,"身体は硬直","身体が硬直","体が硬直","体を硬直","身がこわば","身体がこわば","耐えています","耐えながら","歯を食いしば","我慢しています","身をこわばらせ","必死にこらえ"))return cue(STRAIN,v,2);
  if(has(v,"できました","完了しました","見つかりました","自信があります","胸を張る"))return cue(CONFIDENT,v,1);
  if(has(v,"確認します","調べます","考えます","考えています","検討します","うーん","顎に手を","思案する"))return cue(THINK,v,1);
  if(has(v,"説明します","整理します","順番にお伝え","ご案内します","手のひらを上に"))return cue(EXPLAIN,v,1);
  if(has(v,"教えてください","聞かせてください","ですか？","でしょうか？","耳を傾ける"))return cue(LISTEN,v,0);
  if(has(v,"一緒に歩きます","歩きながら","歩いています","歩き出す","歩き始める"))return cue(WALK,v,1);
  return null;
 }
 private static Cue cue(FullBodyPoseCatalog.Kind kind,String v,int normal){
  int intensity=has(v,"激しく","とても","すごく","大きく","号泣","泣き叫","限界","全身","力尽き","疲れ果て")?3:has(v,"少し","少々","ちょっと","軽く","かすかに")?1:normal;
  return new Cue(kind,intensity);
 }
 private static String clean(String text){
  if(text==null)return "";
  String trimmed=text.trim();
  boolean ownQuoted=trimmed.matches("^[「『“\"][\\s\\S]*[」』”\"]$")||trimmed.startsWith("ロキは");
  String v=ownQuoted?trimmed.replaceAll("[「『“\"」』”]", ""):text.replaceAll("「[^」]*」|『[^』]*』|“[^”]*”|\"[^\"]*\"", "");
  v=v.replace("適切ではありません","不適切です").replace("応じることはできません","応じられません");
  v=v.replaceAll("[^。！？!?]*(?:彼女は|彼は|友人が|患者が|登場人物が)[^。！？!?]*", "");
  // Negated clauses do not assert a reaction; preserve subsequent affirmative sentences.
  return v.replaceAll("[^。！？!?、,]*(?:ではありません|ではない|くありません|くない|ていません|ていない|しません|しない(?!で)|ぐことはありません|ぐことはない)[^。！？!?、,]*", "");
 }
 private static boolean has(String text,String...words){for(String word:words)if(text.contains(word))return true;return false;}
}
