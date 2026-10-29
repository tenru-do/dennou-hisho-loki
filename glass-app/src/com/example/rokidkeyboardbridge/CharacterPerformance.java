package com.example.rokidkeyboardbridge;
/** Cached local choreography: eyes lead head, head leads shoulders. No frame IO. */
final class CharacterPerformance {
 static final int UNDERSTAND=0,AGREE=1,QUESTION=2,NOTES=3;
 private final long[] pauses=new long[64];private int pauseCount,nodKind,mode;private long speechAt=-1,finished=-1,buttonAt=-1,afterContact=-1;private float targetX,targetY;private boolean notes,notesRequested;
 void clear(){mode=0;notesRequested=false;speechAt=finished=buttonAt=afterContact=-1;notes=false;pauseCount=0;}
 void user(String text){finished=afterContact=-1;String t=text==null?"":text;notesRequested=t.matches("(?s).*(メモ|手帳|書き留め|ノート).*" );notes=notesRequested;}
 void answer(String text){String t=text==null?"":text;nodKind=t.matches("(?s).*(でしょうか|ですか[？?]|どちら|[？?]$).*" )?QUESTION:t.matches("(?s).*(その通り|そうですね|同感|賛成).*" )?AGREE:UNDERSTAND;notes=notesRequested||t.matches("(?s).*(メモ|手帳|書き留め|ノート|要点|整理します).*" );pauseCount=0;long cursor=0;for(int i=0;i<Math.min(4000,t.length());i++){char c=t.charAt(i);cursor+=90;if("、。！？!?\n".indexOf(c)>=0){if(pauseCount<pauses.length)pauses[pauseCount++]=cursor;cursor+=c=='、'?220:420;}}}
 void mode(long now,int next){if(next==mode)return;if(next==2){speechAt=now;finished=-1;}else if(mode==2)finished=now;mode=next;}
 void button(float x,float y,long now){targetX=Math.max(-1,Math.min(1,x));targetY=Math.max(-1,Math.min(1,y));buttonAt=now;}
 void aftermath(String text,long now){String t=text==null?"":text;if(t.matches("(?s).*(手を離す|手を離した|そっと離す).*" )&&!t.matches("(?s).*(やめ|嫌|触ら|拒否).*" ))afterContact=now;}
 static float pulse(long age,long duration){if(age<0||age>=duration)return 0;float x=age/(float)duration;return (float)Math.sin(Math.PI*x)*(float)Math.sin(Math.PI*x);}
 float buttonGaze(long now){return buttonAt<0?0:targetX*.008f*pulse(now-buttonAt,2300)*CharacterMotionTuning.eyes();}
 float buttonVertical(long now){return buttonAt<0?0:targetY*.008f*pulse(now-buttonAt,2300)*CharacterMotionTuning.eyes();}
 float buttonShoulder(long now){return buttonAt<0?0:targetX*.002f*pulse(now-buttonAt-300,2000)*CharacterMotionTuning.body();}
 boolean buttonActive(long now){return buttonAt>=0&&now>=buttonAt&&now-buttonAt<2300;}
 float buttonTilt(long now){return buttonAt<0?0:targetX*1.4f*pulse(now-buttonAt-150,2150)*CharacterMotionTuning.face();}
 float finish(long now){return finished<0?0:pulse(now-finished,2000);}
 float aftermath(long now){return afterContact<0?0:pulse(now-afterContact,2300);}
 float nod(long now){float p=finish(now);return ((nodKind==QUESTION?-.003f:nodKind==AGREE?.008f:.005f)*p+phraseNod(now))*CharacterMotionTuning.face();}
 float breath(long now){if(mode!=2||speechAt<0)return .5f+.5f*(float)Math.sin(now/1500.0);long age=now-speechAt;return age<500?CharacterGestureLife.smooth(age/500f):.5f+.5f*(float)Math.sin((age-500)/1700.0);}
 float mouth(long now){if(mode!=2||speechAt<0)return 0;long age=now-speechAt;for(int i=0;i<pauseCount;i++){long p=age-pauses[i];if(p>=0&&p<280)return 0;}return (float)(.55+.30*Math.sin(age/110.0)+.15*Math.sin(age/67.0));}
 int beat(long now){if(mode!=2||speechAt<0)return -1;long age=now-speechAt;for(int i=pauseCount-1;i>=0;i--)if(age>=pauses[i]&&age-pauses[i]<900)return i%3;return -1;}
 float phraseNod(long now){int b=beat(now);if(b<0)return 0;long age=now-speechAt,last=0;for(int i=0;i<pauseCount;i++)if(pauses[i]<=age)last=pauses[i];return pulse(age-last,900)*(b==0?.0035f:b==1?-.002f:.001f);}
 float phraseLean(long now){return beat(now)==1?.0015f*CharacterMotionTuning.body():0;}
 boolean notes(){return notes;}boolean explicitNotes(){return notesRequested;}
 int nodKind(){return nodKind;}
 boolean finishing(long now){return finish(now)>.01f||aftermath(now)>.01f;}
 int portraitCell(long now,int lifeKind,long age,int viewMode){
  if(finishing(now))return aftermath(now)>.01f?7:nodKind==QUESTION?15:14;
  if(viewMode==2)return mouth(now)<.03f?14:13;
  if(viewMode==1)return Math.max(0,age)%4000<1600?0:2;
  if(notes&&age>=0&&age<5200)return 8+Math.min(3,(int)(age/1300));
  if(lifeKind==CharacterGestureLife.THINK)return 1;
  if(lifeKind==CharacterGestureLife.LISTEN){int phase=(int)(Math.max(0,age)/1000);return phase==0?0:phase==1?1:phase==2?2:3;}
  if(lifeKind==CharacterGestureLife.INVITE)return age<900?0:age<1900?4:5;
  if(lifeKind==CharacterGestureLife.JOY)return 14;
  if(lifeKind==CharacterGestureLife.CORRECT)return 2;
  return -1;
 }
}
