package com.example.rokidkeyboardbridge;
import java.text.Normalizer;
import java.util.*;
/** Bounded local interpretation of supported visual controls. Never alters AI input. */
final class VisualConversationPlan {
 static final int PASS=0,HOLD=1,ACTION=2,AFTER_SPEECH=3,QUESTION=4,ADJUST=5,PREFERENCE=6,UNSUPPORTED=7;
 final int kind;final String[] steps;final String reply;final boolean sequence;final int hand,gaze;final float amplitude,speed;
 VisualConversationPlan(int k,String[] s,String r,boolean q,int h,int g,float a,float v){kind=k;steps=s;reply=r;sequence=q;hand=h;gaze=g;amplitude=a;speed=v;}
 static VisualConversationPlan result(int k,String r){return new VisualConversationPlan(k,new String[0],r,false,0,0,1,1);}
 static String clean(String raw){return Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFKC).trim().replaceFirst("^ロキ[、, :：]*","").replace("ねそべ","寝そべ").replace("すわって","座って").replace("腰掛け","腰かけ").replace("腰をおろ","腰を下ろ");}
 static boolean has(String s,String... words){for(String w:words)if(s.contains(w))return true;return false;}
 static VisualConversationPlan resolve(String raw,int posture,String last){
  String t=clean(raw);if(t.isEmpty()||t.length()>240)return result(PASS,"");
  // Corrections refer to the most recent clause; never run abandoned instructions.
  String[] corrections=t.split("(?:やっぱり|やはり|ではなく|じゃなく|ではなくて)");if(corrections.length>1)t=corrections[corrections.length-1].trim();
  if(t.matches("(?:うん|はい|いいね|ありがとう|その姿勢は楽[?？]?|そのままでいい|了解|そうだね)[。!！?？\\s]*"))return result(HOLD,"");
  if(t.matches("(?s).*(例文|小説|写真|画像|友人|彼女|彼が|私は|僕は|私が|僕が|と言って|と言った|について|意味|検索|予定|メール|ニュース).*"))return result(HOLD,"");
  if(has(t,"座っている","寝そべっている","立っている","座ってる","寝そべってる","立ってる"))return result(HOLD,"");
  if(has(t,"昨日","さっきは","以前は")&&!t.matches("(?s).*(座って|寝そべって|立って|腰かけて|横になって)[。！!\\s]*$"))return result(HOLD,"");
  boolean poseWord=has(t,"座","寝そべ","横にな","腰かけ","腰を下ろ","立","くつろ","楽に","手を","向いて","膝","脚","足");
  if(poseWord&&has(t,"できる?","できる？","できるの","座れる","寝そべれる","可能?","可能？"))return result(QUESTION,"座位・立位・寝姿や、用意した仕草を表示できます。姿勢を指定してください。");
  if(has(t,"この座り方が好き","この姿勢が好き","この姿勢を覚えて","この座り方を覚えて"))return result(PREFERENCE,"この姿勢の種類を端末内の好みに保存します。");
  if(has(t,"動きは控えめ","動きを控えめ","もっとゆっくり","ゆっくり動いて","少しだけ動","大きく動","もっと大きく","動きを小さく","通常の速さ","普通の速さ"))return new VisualConversationPlan(ADJUST,new String[0],"動きの強さ・速さを調整します。",false,0,0,has(t,"控えめ","小さく","少しだけ")?.55f:has(t,"大きく")?1.2f:1,has(t,"ゆっくり")?.65f:1);
  if(has(t,"今の動きを戻して","さっきの姿勢に戻って"))t="元の姿勢に戻って";
  if(t.matches("(?:逆|反対|反対の手で|もう少し|さっきの方|さっきのほう|元の姿勢に戻って)[。!！\\s]*")){
   if(has(t,"さっきの","元の"))return new VisualConversationPlan(ACTION,new String[]{"一つ前の姿勢に戻して"},"一つ前の姿勢へ戻します。",false,0,0,1,1);
   if(last==null||last.isEmpty())return result(QUESTION,"どの動作を調整しますか？ 姿勢や手を指定してください。");
   return new VisualConversationPlan(ACTION,new String[]{last},has(t,"もう少し")?"直前の動きを少し強めます。":"直前の動きの向きを変えます。",false,has(t,"反対","逆")?2:0,0,has(t,"もう少し")?1.2f:1,1);
  }
  boolean after=has(t,"話し終わったら","話が終わったら","読み上げが終わったら","返事のあとに");
  if(after)t=t.replaceAll("話し終わったら|話が終わったら|読み上げが終わったら|返事のあとに","");
  boolean sequence=has(t,"てから","そのあと","それから","その後");
  // Subjective complaints are repair, not a request to prohibit the posture.
  if(has(t,"まだ座ってない","座っていない","座ってないよ","座ってないな","立ったままだよ")&&last!=null&&has(last,"座","腰"))t="座って";
  if(has(t,"座ってないよ","座ってないな","まだ座ってない"))t="座って";
  if(has(t,"寝そべらないな","寝そべってない","まだ寝てない"))t="寝そべって";
  if(posture==2&&has(t,"寝そべらないで","横にならないで"))return new VisualConversationPlan(ACTION,new String[]{"座って"},"寝姿をやめ、座る姿へ切り替えます。",false,0,0,1,1);
  if(has(t,"座らないで","座らなくていい","立たないで","立たなくていい","寝そべらないで","横にならないで"))return result(HOLD,"姿勢は変えずに保ちます。");
  if(has(t,"腰かけて","腰を下ろして","腰を落ち着けて","腰をおろして","座ってくれる","座ってもらえる","座ってみて","座ってほしい","座ってくれない"))t=t.replaceAll("腰かけて|腰を下ろして|腰を落ち着けて|座ってくれる|座ってもらえる|座ってみて|座ってほしい|座ってくれない","座って");
  if(has(t,"楽にして","楽な姿勢で","少し休んで","くつろいで","リラックスして"))t=t.replaceAll("楽にして|楽な姿勢で|少し休んで|くつろいで|リラックスして",posture==2?"寝たまま背伸びして":posture==1?"膝を抱えて座って":"座って");
  t=t.replaceAll("寝転んでみて|寝転がってみて|横になってくれる|寝そべってくれる|寝そべってもらえる","寝そべって").replaceAll("立ち上がってくれる|立ち上がってみて|立ってくれる","立って");
  int hand=has(t,"反対の手")?2:has(t,"画面の右","こちらの右")?-1:has(t,"画面の左","こちらの左")?1:has(t,"右手")?1:has(t,"左手")?-1:0;
  int gaze=has(t,"こっち向いて","こちらを見て","こちらを見る","こっちを見て")?1:0;
  List<String> steps=new ArrayList<>();
  String[] clauses=t.split(sequence?"(?:てから|そのあと|それから|その後)[、,\\s]*":"[、,]|そして|ながら|のまま");
  // Detect compound posture even if a comma is omitted.
  for(String c:clauses){
   if(ComfortPoseIntent.adjacent(c)){steps.add(c);}
   else if(c.contains("膝を抱え")&&(has(c,"座","隣")||posture==1||steps.toString().contains("座って"))){steps.add("膝を抱えて座って");}
   else if(has(c,"寝そべって","横になって","寝転んで","寝転がって")||sequence&&has(c,"寝そべ","横にな"))steps.add("寝そべって");
   else if(has(c,"座って","座り","腰かけ","腰を下ろ")||sequence&&c.endsWith("座っ"))steps.add("座って");
   else if(has(c,"立って","立ち上がって")||sequence&&c.endsWith("立っ"))steps.add("立って");
   if(has(c,"手を振","手をふ","右手を振","左手を振"))steps.add("手を振って");
   if(has(c,"脚を組み替","足を組み替","脚を組みかえ","足を組みかえ"))steps.add("脚を組み替えて");
  }
  if(has(t,"差し出して","差し伸べて","手を伸ばして")){if(posture==2)return result(UNSUPPORTED,"寝姿のまま手を差し出す動作は未対応です。今の姿勢を保ちます。");steps.add("手を差し出して");}
  if(has(t,"耳たぶを触って","イヤリングを触って","耳飾りを触って"))steps.add("イヤリングを触って");
  boolean genericPose=steps.size()>0;
  // Preserve rich existing supported commands rather than replacing them with base posture.
  if(!sequence&&EmbodiedScenePolicy.visualCommand(t)&&!genericPose){steps.add(t);}
  if(!sequence&&steps.size()==1&&!has(t,"、",",")&&has(t,"寝たまま","座ったまま","困った顔","切ない顔","膝を抱え","ストレッチ","枕を抱え")){steps.clear();steps.add(t);}
  if(gaze>0&&steps.isEmpty())return new VisualConversationPlan(ADJUST,new String[0],"今の姿勢のまま、こちらへ視線を向けます。",false,hand,gaze,1,1);
  if(posture==2&&steps.size()==1&&steps.get(0).equals("手を振って"))steps.set(0,"寝たまま手を振って");
  if(posture==2&&has(t,"そのまま","寝たまま")&&steps.contains("脚を組み替えて"))return result(UNSUPPORTED,"寝姿での脚の組み替えは未対応です。今の姿勢を保ちます。");
  if(steps.isEmpty()){
   if(has(t,"ポーズ","姿勢","あぐら","倒立","四つん這い")&&t.matches("(?s).*(して|とって|なって|向けて|見せて)[。!！?？\\s]*"))return result(UNSUPPORTED,"その動作はまだ表示に対応していません。今の姿勢を保ちます。");
   return result(PASS,"");
  }
  if(steps.size()>4)return result(QUESTION,"動作は一度に4つまで指定してください。");
  float a=has(t,"少しだけ","小さく","控えめ")?.55f:has(t,"大きく")?1.2f:1;float v=has(t,"ゆっくり")?.65f:1;
  return new VisualConversationPlan(after?AFTER_SPEECH:ACTION,steps.toArray(new String[0]),after?"読み上げが終わったら姿勢を切り替えます。":steps.size()>1?"今の指示に合わせ、順に表示を切り替えます。":"",sequence,hand,gaze,a,v);
 }
}
