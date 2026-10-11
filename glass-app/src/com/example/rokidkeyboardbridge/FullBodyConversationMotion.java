package com.example.rokidkeyboardbridge;
/** One reaction per utterance. Never invent laughter from polite thanks. */
final class FullBodyConversationMotion {
 private static final int[] TEAR_POSES={288,289,290,291,289,292};
 private static final long[] TEAR_DURATIONS={500,400,650,350,400};
 private static final long[] DURATIONS={180,180,600,550,380};
 static int kind(FullBodyPoseCatalog.Kind emotion,boolean laughing,boolean seated){
  if(emotion==FullBodyPoseCatalog.Kind.CRY)return 2;
  if(!seated)return -1;
  if(emotion==FullBodyPoseCatalog.Kind.SURPRISE)return 0;
  return emotion==FullBodyPoseCatalog.Kind.JOY&&laughing?1:-1;
 }
 static int kind(FullBodyPoseCatalog.Kind emotion,boolean laughing,boolean seated,String text){
  int normal=kind(emotion,laughing,seated);
  if(normal>=0)return normal;
  return emotion==FullBodyPoseCatalog.Kind.THINK?FullBodySpeechStance.thoughtMotion(text):-1;
 }
 static boolean continues(int kind,FullBodyPoseCatalog.Kind emotion,boolean laughing){
  return kind==3?emotion==FullBodyPoseCatalog.Kind.THINK:kind>=4?false:kind(emotion,laughing,true)==kind;
 }
 static int first(int kind){return kind==0?276:kind==1?282:kind==2?288:kind==3?293:299;}
 static int[] frames(int kind){if(kind==2)return TEAR_POSES.clone();int n=first(kind);return new int[]{n,n+1,n+2,n+3,n+4,n+5};}
 static int pose(int kind,int frame){return kind==2?TEAR_POSES[frame]:first(kind)+frame;}
 static int frame(long elapsed){return frame(0,elapsed);}
 static int frame(int kind,long elapsed){long[] durations=kind==2?TEAR_DURATIONS:DURATIONS;long t=Math.max(0,elapsed);for(int i=0;i<durations.length;i++){if(t<durations[i])return i;t-=durations[i];}return 5;}
}
