package com.example.rokidkeyboardbridge;
/** Gentle, non-explicit eye contact and proximity. Never changes posture or consent. */
final class WarmPresencePolicy {
 static final int NONE=0,LOOK=1,SHY=2,NEAR=3,HAND=4,KISS_FACE=5,STOP=6;
 private int kind;private long started=-1,last=-30000;
 static int command(String text){if(text!=null&&text.trim().matches("(?:ロキ|ねえ[、, ]*ロキ)[。！!\\s]*"))return LOOK;String t=VisualConversationPlan.clean(text).replaceAll("[。！!\\s]+$","");if(t.length()>60)return NONE;
  if(t.matches("(?:親密な演出を止めて|その演出をやめて|距離を戻して|近づかないで|手を離して)"))return STOP;
  if(t.matches("(?:こっち(?:を)?見て|こちら(?:を)?見て|目を合わせて|見つめて|ねえ|呼んだよ)"))return LOOK;
  if(t.matches("(?:かわいい(?:ね)?|可愛い(?:ね)?|少し照れて|照れた顔をして|色っぽい表情にして|少し色っぽくして|大人っぽく微笑んで|優しく微笑んで)"))return SHY;
  if(t.matches("(?:少し近くに来て|少しだけ近づいて|そばにいて|寄り添って|甘えて|少し甘えて|隣にいて|もっと近くに|こっちにおいで)"))return NEAR;
  if(t.matches("(?:手をつないで|手を取って|手を握って)"))return HAND;
  if(t.matches("(?:軽いキス顔にして|優しいキス顔にして|投げキスして)"))return KISS_FACE;
  return NONE;
 }
 static boolean followup(String text){return VisualConversationPlan.clean(text).replaceAll("[。！!\\s]+$", "").matches("(?:うん|はい|いいね|ありがとう|その感じ|そのまま|続けて|もう少し)");}
 static boolean stop(String text){String t=VisualConversationPlan.clean(text);return command(text)==STOP||t.matches("(?:やめて|離して|もうやめて|普通に戻って)[。！!\\s]*");}
 static String reply(int kind){return kind==LOOK?"はい、マスター。こちらへ目線を向けます。":kind==SHY?"ありがとうございます、マスター。少し照れてしまいますね。":kind==NEAR?"マスター、少しだけ近くへ寄りますね。":kind==HAND?"そっと手を差し出しますね。":kind==KISS_FACE?"マスターへ、軽いキスの表情をお届けします。":"いつもの距離に戻りますね。";}
 void user(String text,long now){if(stop(text)){clear();return;}int next=command(text);if(next==NONE){if(!followup(text))clear();return;}if(CharacterMotionTuning.warmth==0){clear();return;}if(now>=last&&now-last<900&&next==kind)return;kind=next;started=last=now;}
 void clear(){kind=NONE;started=-1;}
 boolean active(long now){if(CharacterChoreography.scene==1){clear();return false;}return CharacterMotionTuning.warmth>0&&started>=0&&now>=started&&now-started<6800;}
 float envelope(long now,int mode,boolean quiet){if(quiet||!active(now))return 0;float t=now-started;float enter=CharacterRapportPolicy.smooth(t/650f),leave=CharacterRapportPolicy.smooth((6800-t)/1500f);float strength=CharacterMotionTuning.warmth==1?.35f:CharacterMotionTuning.warmth==2?.65f:1;return enter*leave*strength*(mode==2?.35f:mode==1?.45f:1);}
 float gaze(long now,int mode,boolean quiet){float age=now-started;return envelope(now,mode,quiet)*(age<1800?0:age<3400?-.30f:.08f);}
 float approach(long now,int mode,boolean quiet){return (kind==NEAR||kind==HAND||kind==KISS_FACE?.014f:.004f)*envelope(now,mode,quiet)*CharacterMotionTuning.approach();}
 float tilt(long now,int mode,boolean quiet){return (kind==SHY?-.65f:.25f)*envelope(now,mode,quiet);}
 float smile(long now,int mode,boolean quiet){return envelope(now,mode,quiet)*(kind==LOOK?.45f:.8f);}
 int portraitCell(long now){return kind==SHY?7:now-started<1800?0:now-started<3400?7:5;}
}
