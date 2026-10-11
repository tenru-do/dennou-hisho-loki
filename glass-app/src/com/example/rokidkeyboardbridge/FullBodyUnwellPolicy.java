package com.example.rokidkeyboardbridge;
/** Ordinary seated unwellness and steady breathing; no idle or standing activation. */
final class FullBodyUnwellPolicy {
 static final String[] ASSETS={"loki_unwell_cover_mouth_v464.png","loki_unwell_steady_breath_v464.png"};
 static boolean eligible(FullBodyPoseCatalog.Kind kind,boolean seated,String text){
  if(!seated||kind==FullBodyPoseCatalog.Kind.REFUSE||kind==FullBodyPoseCatalog.Kind.ANGER)return false;
  String v=FullBodySpeechStance.clean(text).replaceAll("[^。！？!?]*(?:もし|仮に|としたら|場合は|という仮定|あなたは|あなたが|友人は|患者は|彼女が|彼が|登場人物は|マスターは|ユーザーは)[^。！？!?]*", "");
  if(v.contains("について説明")||v.contains("について教")||v.contains("という意味"))return false;
  return v.contains("吐き気がします")||v.contains("気分が悪いです")||v.contains("体調が悪いです")||v.contains("吐き気を感じています");
 }
 static int frame(long elapsed){return Math.max(0,elapsed)<900L?0:1;}
 static int previous(long elapsed){return 0;}
 static float blend(long elapsed){return Math.max(0,elapsed)<900L?1:Math.min(1,(Math.max(0,elapsed)-900L)/180f);}
}
