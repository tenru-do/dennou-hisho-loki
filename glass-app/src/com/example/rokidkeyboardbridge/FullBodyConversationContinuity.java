package com.example.rokidkeyboardbridge;
/** Holds a visual reaction across speaking, listening and network/TTS gaps. */
final class FullBodyConversationContinuity {
 static final long GAP_MS=180000L;
 private long touched=-1;
 private boolean active;
 private int pose=-1,intensity;
 private FullBodyPoseCatalog.Kind kind;
 void touch(long now){if(!engaged(now)){pose=-1;kind=null;intensity=0;}touched=Math.max(0,now);}
 void active(boolean value,long now){if(value||active)touch(now);active=value;}
 boolean engaged(long now){return active||(touched>=0&&now>=touched&&now-touched<GAP_MS);}
 boolean holds(long now){return pose>=0&&engaged(now);}
 int pose(){return pose;}
 FullBodyPoseCatalog.Kind kind(){return kind;}
 int accept(int candidate,FullBodyPoseCatalog.Kind next,int strength,boolean posture,long now){
  if(!engaged(now))clear();
  touch(now);
  if(candidate>=0&&(pose<0||posture||kind!=next||intensity!=strength)){pose=candidate;kind=next;intensity=strength;}
  return pose;
 }
 void clear(){pose=-1;kind=null;intensity=0;touched=-1;active=false;}
 static boolean posture(String text){String v=FullBodySpeechStance.clean(text);return FullBodyScenePolicy.has(v,"座って","座らせて","お座り","椅子に座","座ったまま","腰掛け","腰かけ","立ち上が","立ったまま");}
 static boolean end(String text){String v=FullBodyScenePolicy.clean(text);return FullBodyScenePolicy.has(v,"会話を終了","会話を終わり","会話をやめ","対話を終了","今日はここまで","またね","おやすみ");}
 static boolean topic(String text){return FullBodyScenePolicy.has(FullBodyScenePolicy.clean(text),"別の話","話題を変","天気","予定","ニュース","について教","とは何","相談","ごめん","謝","すみません","もうやめます");}
}
