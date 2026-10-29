package com.example.rokidkeyboardbridge;
/** Visual-only anticipation, invitation, acknowledgement and settling; no app command. */
final class CharacterRapportPolicy {
 final CharacterGestureLife life=new CharacterGestureLife();
 static final long DURATION=6200,WAIT_START=900,WAIT_END=4300;
 private long started=-1,accepted=-1;private int style,sequence;private String prompt="",lastAnswer="";private boolean offer;
 void clear(){life.clear();started=accepted=-1;offer=false;prompt=lastAnswer="";}
 void user(String text,long now){
  String next=text==null?"":text;if(next.equals(prompt))return;
  prompt=next;lastAnswer="";life.performance.user(next);life.event(CharacterGestureLife.LISTEN,now);int request=FullBodyAffectionCue.request(next);offer=request==2||request==3;
  started=accepted=-1;if(FullBodyAffectionCue.stop(next)||ConversationTopicBoundary.reset(next)){clear();life.performance.aftermath(next,now);}
 }
 void answer(String text,FullBodyPoseCatalog.Kind kind,long now){
  if(text==null||text.isEmpty()||text.equals(lastAnswer))return;lastAnswer=text;life.answer(text,kind,now);
  if(CharacterChoreography.emotionalPriority(kind)||FullBodyAffectionCue.negative(kind)||FullBodyAffectionCue.stop(text)){started=accepted=-1;offer=false;return;}
  if(offer)style=2;
  else if(prompt.matches("(?s).*(ロキ.*(?:すごい|可愛い|かわいい|ありがとう|上手)|褒めて|よくできた|助かった).*"))style=1;
  else if(kind==FullBodyPoseCatalog.Kind.THINK||text.matches("(?s).*(もう一度|どちら|でしょうか|ですか[？?]).*"))style=3;
  else if(kind==null||kind==FullBodyPoseCatalog.Kind.NEUTRAL||kind==FullBodyPoseCatalog.Kind.EXPLAIN||kind==FullBodyPoseCatalog.Kind.JOY||kind==FullBodyPoseCatalog.Kind.SHY||kind==FullBodyPoseCatalog.Kind.CONFIDENT)style=0;
  else {started=accepted=-1;return;}
  sequence++;started=now;accepted=-1;
 }
 void preview(long now){offer=true;style=2;sequence++;started=now;accepted=-1;lastAnswer="";}
 boolean active(long now){return started>=0&&now>=started&&now-started<DURATION;}
 boolean waiting(long now){return active(now)&&offer&&accepted<0&&now-started>=WAIT_START&&now-started<WAIT_END||!active(now)&&life.invitationFor(now,0,false)>.1f;}
 boolean touch(long now){if(!waiting(now))return false;if(!active(now)){started=now;offer=true;style=2;}accepted=now;life.event(CharacterGestureLife.JOY,now);return true;}
 int cell(long now){if(!active(now))return -1;if(accepted>=0)return 7;if(style==1)return now-started<1700?7:3;if(style==2)return now-started<WAIT_END?1:7;return style==3?1:5;}
 int bodyClip(long now,boolean seated){int cue=life.bodyClip(now,seated);if(life.performance.finishing(now))return cue;return active(now)&&offer?FullBodyEverydayGesture.clip(seated,2):cue>=0?cue:!active(now)&&life.invitationFor(now,0,false)>.01f?FullBodyEverydayGesture.clip(seated,3):-1;}
 long bodyTime(long now){if(life.performance.finishing(now))return 3900;return life.bodyClip(now,false)>=0?life.bodyTime(now):active(now)?Math.max(0,now-started):Math.max(0,now%Math.max(1,CharacterGestureLife.invitationPeriod())-15000);}

 float envelope(long now,int mode,boolean quiet){
  if(quiet||!active(now))return 0;float t=now-started;
  float enter=smooth(t/900f),leave=smooth((DURATION-t)/1600f);
  float value=enter*leave;if(accepted>=0){float a=(now-accepted)/1000f;value*=1+.25f*(float)Math.sin(Math.min(1,a)*Math.PI);}
  return value*CharacterMotionTuning.scale()*(mode==2?.22f:mode==1?.35f:1f);
 }
 static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
 float side(){return CharacterMotionTuning.variant(sequence)%2==0?-1:1;}
 float lean(long now,int mode,boolean quiet){return envelope(now,mode,quiet)*side()*(style==1?.8f:style==3?.55f:1);}
 float gaze(long now,int mode,boolean quiet){if(!active(now))return 0;return lean(now,mode,quiet)*(now-started<WAIT_END?1:-.55f);}
 void deform(float[] mesh,int columns,int rows,int w,int h,long now,int mode,boolean quiet,float faceBottom){
  life.deform(mesh,columns,rows,w,h,now,mode,quiet,faceBottom);
  float lean=lean(now,mode,quiet);if(lean==0)return;
  for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){
   float y=row/(float)rows; // Keep the face overlay and complete lower body anchored.
   if(y<=faceBottom||y>=.60f)continue;
   float band=(float)Math.pow(Math.sin(Math.PI*(y-faceBottom)/(.60f-faceBottom)),2);
   int n=(row*(columns+1)+col)*2;mesh[n]+=w*.018f*lean*band;mesh[n+1]-=h*.006f*Math.abs(lean)*band;
  }
 }
}
