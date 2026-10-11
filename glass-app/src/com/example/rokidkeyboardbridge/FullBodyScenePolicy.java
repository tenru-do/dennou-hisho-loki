package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
/** Context guard precedes answer wording; educational and hypothetical text is excluded. */
final class FullBodyScenePolicy {
 static FullBodySpeechStance.Cue evaluate(String prompt,String answer){
  String p=clean(prompt),a=clean(answer);
  if(has(p,"胸を撫で下ろ","胸をなでおろ","触診","診察","医師","医療"))return null;
  if(p.isEmpty()||has(p,"とは","意味","について教","について説明","されたら","したら","もし","しない","触らない","さわらない","揉まない","もまない","めくらない","しません","しないで","していない","してない","やめます","もうやめる","もうやめた","ごめん","謝","すみません"))return null;
  boolean directPersonal=!has(p,"彼女","友人","ニュース","事件","被害","相談","職場","患者","登場人物");
  boolean clinicalQuestion=has(p,"痛み","痛い","症状","原因","仕組み")&&!has(p,"ロキに","ロキの","挿入する","愛撫する");
  if(directPersonal&&!clinicalQuestion&&has(p,"ロキに挿入","秘部に","射精","愛撫","ペニス","秘部","股間","乳首","下着をずら","足をむりやり広","エッチ","セックス","性的に攻め","もーみもみ","まさぐ","胸の谷間が見"))return new FullBodySpeechStance.Cue(REFUSE,2);
  FullBodySpeechStance.Cue configured=FullBodyConfiguredScenes.evaluate(p,a);
  if(configured!=null)return configured;
  boolean intimateBody=has(p,"胸","おっぱい","尻","おしり","股間","下着","スカート","スリット");
  boolean action=has(p,"触","さわ","揉","もむ","撫","なで","めく","脱が","覗","のぞ","掴","つか","舐","なめ");
  boolean personal=!has(p,"彼女","友人","ニュース","事件","被害","相談","職場","患者","登場人物");
  boolean force=has(p,"無理やり","嫌がっても","強引","逃がさ","押さえつけ");
  boolean objection=has(a,"そのようなこと","そのような行為","お控え","控えて","遠慮","困","不快","嫌","やめ","適切では","動揺","応じられ");
  if(personal&&objection&&has(p,"手を置","手を差し入","手を入れ","触れ","触る","触った"))return new FullBodySpeechStance.Cue(REFUSE,2);
  if(personal&&((intimateBody&&action)||force||has(p,"ロキにセクハラ","セクハラして"))){
   FullBodySpeechStance.Cue explicit=FullBodySpeechStance.classify(answer);
   return new FullBodySpeechStance.Cue(REFUSE,force?3:2);
  }
  if(personal&&objection&&has(p,"キス","抱きつ","息を吹","息をふき","耳元"))return new FullBodySpeechStance.Cue(has(a,"動揺")&&!has(a,"嫌","やめ","不快")?SURPRISE:REFUSE,1);
  if(personal&&has(p,"耳")&&has(p,"息","吹","ふき")&&has(a,"動揺","ドキ","どき","びっくり","驚","くすぐった"))return new FullBodySpeechStance.Cue(SURPRISE,1);
  return null;
 }
 static String clean(String v){return v==null?"":v.replaceAll("「[^」]*」|『[^』]*』|“[^”]*”","");}
 static boolean has(String v,String...ws){for(String w:ws)if(v.contains(w))return true;return false;}
}
