package com.example.rokidkeyboardbridge;
/** Standing first-person refusal, one gesture per utterance, no idle repetition. */
final class FullBodyRearStopPolicy {
 static final String[] ASSETS={"loki_rear_stop_base_v463.png","loki_rear_stop_prepare_v463.png","loki_rear_stop_push_v463.png"};
 private static final int[] FRAMES={1,0,2,0,1,0};
 private static final long[] DURATIONS={240,260,650,260,240};
 static boolean eligible(FullBodyPoseCatalog.Kind kind,boolean seated,String text){
  String asserted=text==null?null:text.replaceAll("[^。！？!?]*(?:(?:拒む|拒絶する|制止する)ことは(?:ありません|ない)|もし|仮に|としたら|という仮定)[^。！？!?]*", "");
  FullBodySpeechStance.Cue explicit=FullBodySpeechStance.classify(asserted);
  return !seated&&kind==FullBodyPoseCatalog.Kind.REFUSE&&explicit!=null&&explicit.kind==FullBodyPoseCatalog.Kind.REFUSE;
 }
 static int step(long elapsed){long t=Math.max(0,elapsed);for(int i=0;i<DURATIONS.length;i++){if(t<DURATIONS[i])return i;t-=DURATIONS[i];}return 5;}
 static int frame(long elapsed){return FRAMES[step(elapsed)];}
 static int previous(long elapsed){return FRAMES[Math.max(0,step(elapsed)-1)];}
 static float blend(long elapsed){int step=step(elapsed);if(step==0)return 1;long t=Math.max(0,elapsed);for(int i=0;i<step;i++)t-=DURATIONS[i];return Math.min(1,t/90f);}
}
