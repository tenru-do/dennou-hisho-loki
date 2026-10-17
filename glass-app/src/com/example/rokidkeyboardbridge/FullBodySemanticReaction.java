package com.example.rokidkeyboardbridge;
import java.util.*;
import java.util.regex.*;
/** Optional reply metadata. Text transport and function calling stay compatible. */
final class FullBodySemanticReaction {
 static final String INSTRUCTION="全身表示の反応情報：回答本文の意味と直前の会話から、ロキ自身の反応を判定してください。本文の末尾に一行だけ [[LOKI_REACTION:感情:強さ:動作]] を付けてください。感情はNEUTRAL,LISTEN,EXPLAIN,THINK,JOY,CONFIDENT,ANGER,SAD,CRY,SURPRISE,SHY,WORRY,REFUSE,TIRED,RECOVERY,STRAINのいずれか。強さは0〜3。動作はDEFAULT,REST,PROTECT,STARTLE,THINK,EXPLAIN,SMILEのいずれか。困る・戸惑う返答はWORRYとDEFAULT。明確に拒絶・制止する返答はREFUSEとPROTECT、驚きはSURPRISEとSTARTLE。単語の有無だけで判断せず、否定・引用・第三者の話を区別する。実際に反応がない時はNEUTRAL:0:REST。姿勢の立位・座位は変更しない。この情報は本文に混ぜず、内部メタデータとして最後に付ける。"+
  "場面動作も選べます："+FullBodyConversationScenes.guidance()+
  "場面動作はロキ自身がその状態にあるか、ユーザーがロキの演技・ポーズを明示的に依頼した場合だけ指定してください。症状の相談、第三者、ニュース、説明、仮定、否定からロキの不調を作らないでください。該当時は上記の動作欄へSCENE_名を入れます。落ち着いた・回復した状態はRECOVERY:1:REST。場面が続く相づちには同じ場面を保持し、説明や笑顔へ突然戻さないでください。本文は自然な返答にし、選択のために『私は』などの定型文を付け足す必要はありません。BREATH,PANTも呼吸状態の感情として使えます。";
 static final class Reaction {final FullBodySpeechStance.Cue cue;final String action;Reaction(FullBodySpeechStance.Cue cue,String action){this.cue=cue;this.action=action;}}
 private static final Pattern TAG=Pattern.compile("\\[\\[(?:LOKI_REACTION:)?([A-Z]+):([0-3]):(DEFAULT|REST|PROTECT|STARTLE|THINK|EXPLAIN|SMILE|SCENE_[A-Z_]+)\\]\\]");
 private static final LinkedHashMap<String,Reaction> recent=new LinkedHashMap<>();
 static synchronized String decode(String raw){
  if(raw==null)return "";
  Matcher matcher=TAG.matcher(raw);Reaction reaction=null;
  while(matcher.find())try{if(matcher.group(3).startsWith("SCENE_")&&FullBodyConversationScenes.action(matcher.group(3))==null)continue;reaction=new Reaction(new FullBodySpeechStance.Cue(FullBodyPoseCatalog.Kind.valueOf(matcher.group(1)),Integer.parseInt(matcher.group(2))),matcher.group(3));}catch(IllegalArgumentException ignored){}
  String prefix="(?:LOKI_REACTION:|(?:NEUTRAL|LISTEN|EXPLAIN|THINK|JOY|CONFIDENT|ANGER|SAD|CRY|SURPRISE|SHY|WORRY|REFUSE|TIRED|RECOVERY|STRAIN|BREATH|PANT):)";
  String text=raw.replaceAll("\\[\\["+prefix+"[^\\r\\n]*?\\]\\]", "").replaceAll("\\[\\["+prefix+"[^\\r\\n]*$", "").trim();
  text=ConversationReplyPolicy.clean(text);
  if(reaction==null&&!text.isEmpty())recent.remove(text);
  if(reaction!=null&&!text.isEmpty()){recent.put(text,reaction);while(recent.size()>32)recent.remove(recent.keySet().iterator().next());}
  return text;
 }
 static synchronized Reaction find(String text){return recent.get(text==null?"":text.trim());}
 static int actionPose(int fallback,FullBodySpeechStance.Cue cue,String action,int current){
  if(cue==null||fallback<0||action==null||"DEFAULT".equals(action))return fallback;
  String wanted="PROTECT".equals(action)?"protective":"STARTLE".equals(action)?"raised":"THINK".equals(action)?"chin":"EXPLAIN".equals(action)?"palm_up":"SMILE".equals(action)?"smiling":"REST".equals(action)?"arms_down":"";
  if(wanted.isEmpty())return fallback;boolean seated=current>=0&&FullBodyMotionPolicy.isSeated(current);
  int selected=-1,best=1000;
  for(FullBodyPoseCatalog.Pose p:FullBodyPoseCatalog.ALL)if(p.supports(cue.kind)&&p.seated==seated&&p.gesture.contains(wanted)){
   int score=Math.abs(p.intensity-cue.intensity);if(score<best){best=score;selected=p.id;}
  }
  return selected>=0?selected:fallback;
 }
}
