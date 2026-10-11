package com.example.rokidkeyboardbridge;
import java.util.*;
import java.util.regex.*;
/** Optional reply metadata. Text transport and function calling stay compatible. */
final class FullBodySemanticReaction {
 static final String INSTRUCTION="全身表示の反応情報：回答本文の意味と直前の会話から、ロキ自身の反応を判定してください。本文の末尾に一行だけ [[LOKI_REACTION:感情:強さ:動作]] を付けてください。感情はNEUTRAL,LISTEN,EXPLAIN,THINK,JOY,CONFIDENT,ANGER,SAD,CRY,SURPRISE,SHY,WORRY,REFUSE,TIRED,RECOVERY,STRAINのいずれか。強さは0〜3。動作はDEFAULT,REST,PROTECT,STARTLE,THINK,EXPLAIN,SMILEのいずれか。拒否・境界を示す返答はREFUSEとPROTECT、驚きはSURPRISEとSTARTLE。単語の有無だけで判断せず、否定・引用・第三者の話を区別する。実際に反応がない時はNEUTRAL:0:REST。姿勢の立位・座位は変更しない。この情報は本文に混ぜず、内部メタデータとして最後に付ける。";
 static final class Reaction {final FullBodySpeechStance.Cue cue;final String action;Reaction(FullBodySpeechStance.Cue cue,String action){this.cue=cue;this.action=action;}}
 private static final Pattern TAG=Pattern.compile("\\[\\[LOKI_REACTION:([A-Z]+):([0-3]):(DEFAULT|REST|PROTECT|STARTLE|THINK|EXPLAIN|SMILE)\\]\\]");
 private static final LinkedHashMap<String,Reaction> recent=new LinkedHashMap<>();
 static synchronized String decode(String raw){
  Matcher matcher=TAG.matcher(raw);Reaction reaction=null;
  while(matcher.find())try{reaction=new Reaction(new FullBodySpeechStance.Cue(FullBodyPoseCatalog.Kind.valueOf(matcher.group(1)),Integer.parseInt(matcher.group(2))),matcher.group(3));}catch(IllegalArgumentException ignored){}
  String text=raw.replaceAll("\\[\\[LOKI_REACTION:[^\\r\\n]*?\\]\\]", "").replaceAll("\\[\\[LOKI_REACTION:[^\\r\\n]*$", "").trim();
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
