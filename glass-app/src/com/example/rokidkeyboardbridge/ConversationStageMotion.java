package com.example.rokidkeyboardbridge;
/** Shared event-relative presentation only; never edits model inputs or executes app actions. */
final class ConversationStageMotion {
 private static int phase=-1;private static long changed,finished=-1,epoch;private static float speechGaze;private static boolean online=true;
 static void transport(boolean available){online=available;}
 static void update(int next,long now){if(next==phase&&now>=changed)return;float before=gaze(now,false);if(phase==CharacterChoreography.SPEAK&&next==CharacterChoreography.IDLE)finished=now;else if(next!=CharacterChoreography.IDLE)finished=-1;if(next==CharacterChoreography.SPEAK)speechGaze=before;phase=next;changed=now;epoch++;}
 static long epoch(){return epoch;}static boolean current(long ticket){return ticket==epoch;}
 static float ease(float v){v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
 static float finish(long now){return finished<0||now<finished||now-finished>=1700?0:(float)Math.sin(Math.PI*(now-finished)/1700);}
 static float gaze(long now,boolean quiet){if(quiet||phase==CharacterChoreography.THINK&&!online)return 0;long age=Math.max(0,now-changed);if(phase==CharacterChoreography.THINK){float fade=1-ease((age-8000)/8000f);return -.30f*ease((age-250)/750f)*fade;}if(phase==CharacterChoreography.SPEAK)return speechGaze*(1-ease(age/420f));return 0;}
 static float nod(long now,boolean quiet){return quiet?0:.30f*finish(now);}
 static float waitGain(long now){if(phase!=CharacterChoreography.THINK)return 1;if(!online)return .15f;return 1-.8f*ease((Math.max(0,now-changed)-8000)/8000f);}
 static boolean settling(long now){return finish(now)>0;}
 static int meaning(String text,FullBodyPoseCatalog.Kind kind){
  String t=text==null?"":text.trim();
  if(kind==FullBodyPoseCatalog.Kind.REFUSE||kind==FullBodyPoseCatalog.Kind.ANGER)return CharacterGestureLife.SETTLE;
  if(t.matches("(?s)^(?:はい[、。 ]*)?(?:マスター[、,。 ]*)?(?:ここから|次の角|まっすぐ|右へ|左へ).*"))return CharacterGestureLife.GUIDE;
  if(t.matches("(?s)^(?:マスター[、,。 ]*)?(?:申し訳|失礼|すみません|訂正|先ほどの.*(?:誤|間違)).*"))return CharacterGestureLife.CORRECT;
  if(kind==FullBodyPoseCatalog.Kind.JOY)return CharacterGestureLife.JOY;
  if(kind==FullBodyPoseCatalog.Kind.THINK)return CharacterGestureLife.THINK;
  if(t.matches("(?s)^(?:マスター[、,。 ]*)?(?:はい[、。 ]*)?(?:その通り|おっしゃる通り|確認できました|承知しました|承知いたしました).*"))return CharacterGestureLife.LISTEN;
  if(kind==FullBodyPoseCatalog.Kind.EXPLAIN||t.matches("(?s).*(?:まず[、,]|次に[、,]|理由は|手順は|要点は|具体的には).*"))return CharacterGestureLife.EXPLAIN;
  return CharacterGestureLife.SETTLE;
 }
 static boolean optional(long now,boolean explicit){return explicit||!settling(now)&&!(phase==CharacterChoreography.THINK&&(!online||now-changed>8000));}
 private ConversationStageMotion(){}
}
