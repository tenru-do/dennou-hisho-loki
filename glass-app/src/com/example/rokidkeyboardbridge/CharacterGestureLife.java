package com.example.rokidkeyboardbridge;
/** Visual events only. Uptime clock, no timers, IO, random allocation or scene mutation. */
final class CharacterGestureLife {
 final CharacterPerformance performance=new CharacterPerformance();
 private final CharacterGestureVariation variations=new CharacterGestureVariation();private int selectedFamily=-1;
 static final int LISTEN=0,THINK=1,EXPLAIN=2,GUIDE=3,JOY=4,CORRECT=5,INVITE=6,SETTLE=7,CLARIFY=8;
 static final long DURATION=4400;
 private boolean blocked,visualQuiet;private int visualMode;
 private long idleAge(long now,int mode,boolean quiet){visualMode=mode;visualQuiet=quiet;return ConversationMicroMotion.idleAge(now,at,mode,blocked,quiet);}
 private int idleVariant(long now){return ConversationMicroMotion.idleVariant(now,at);}
 private int kind=SETTLE,previous=-1,sequence;private long at=-1,touchAt=-1;private int touchKind,direction;
 void clear(){variations.clear();selectedFamily=-1;performance.clear();blocked=true;at=touchAt=-1;kind=SETTLE;}
 void event(int requested,long now){event(requested,now,false);}
 void event(int requested,long now,boolean explicit){blocked=false;kind=requested==previous&&requested==JOY?SETTLE:Math.max(0,Math.min(8,requested));previous=kind;sequence++;at=now;int family=performance.notes()?12:kind==THINK?1:kind==EXPLAIN||kind==GUIDE||kind==JOY?0:kind==INVITE?3:-1;selectedFamily=variations.choose(family,now,explicit||performance.explicitNotes());}
 void answer(String text,FullBodyPoseCatalog.Kind semantic,long now){
  if(FullBodyAffectionCue.negative(semantic)){clear();return;}
  String t=text==null?"":text;performance.answer(t);
  int k=t.matches("(?s).*(訂正|間違|誤り|失礼).*" )?CORRECT:
   t.matches("(?s).*(右へ|左へ|右側|左側|曲が|道順|進んで).*" )?GUIDE:
   semantic==FullBodyPoseCatalog.Kind.THINK?THINK:semantic==FullBodyPoseCatalog.Kind.JOY?JOY:semantic==FullBodyPoseCatalog.Kind.EXPLAIN?EXPLAIN:SETTLE;
  event(k,now);
 }
 void touch(long now,int dir,long duration,boolean contact){
  if(!contact)return;
  boolean repeated=touchAt>=0&&now>=touchAt&&now-touchAt<850;
  touchKind=repeated?2:duration>=450?1:0;touchAt=now;direction=dir;
 }
 static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
 static float envelope(long age,long duration){return age<0||age>=duration?0:smooth(age/550f)*smooth((duration-age)/1000f);}
 int kind(){return kind;}
 int bodyClip(long now,boolean seated){if(performance.finishing(now))return FullBodyEverydayGesture.clip(seated,2);if(!moving(now)||at<0||now<at||now-at>=DURATION)return -1;int family=selectedFamily;return family<0?-1:FullBodyEverydayGesture.clip(seated,family);}
 long bodyTime(long now){if(performance.finishing(now))return 3900;long age=Math.max(0,now-at);return (long)((kind==GUIDE?age*3/4:kind==JOY?age*11/10:age)*CharacterGestureVariation.pace(variations.variant()));}
 float invitationFor(long now,int mode,boolean quiet){return blocked?0:invitation(now,mode,quiet);}

 float amount(long now,int mode,boolean quiet){return quiet||at<0?0:envelope(now-at,DURATION)*CharacterMotionTuning.scale()*(mode==1?.65f:mode==2?.38f:1);}
 float touch(long now,boolean quiet){if(quiet||touchAt<0)return 0;long age=now-touchAt;float e=envelope(age,touchKind==1?2200:1500);return e*CharacterMotionTuning.scale()*(touchKind==2?(float)Math.sin(age/95.0)*.65f:touchKind==1?.55f:1);}
 float tilt(long now,int mode,boolean quiet){performance.mode(now,mode);visualMode=mode;visualQuiet=quiet;float a=quiet?0:amount(now,mode,false)*smooth(Math.max(0,now-at-150)/450f)*CharacterMotionTuning.face();float micro=quiet?0:(ConversationMicroMotion.tilt(kind,age(now),sequence)+ConversationMicroMotion.idleTilt(idleAge(now,mode,quiet),idleVariant(now)))*CharacterMotionTuning.face();return limit(micro+a*(kind==CLARIFY?1.8f:kind==THINK?-2.1f:kind==GUIDE?1.4f:kind==JOY?1.6f:kind==LISTEN?-1.1f:.45f)+touch(now,quiet)*(direction==2?-1:1)*CharacterMotionTuning.face()+(quiet?0:performance.buttonTilt(now)),2.85f);}
 static float limit(float value,float max){return Math.max(-max,Math.min(max,value));}
 float nod(long now,int mode,boolean quiet){performance.mode(now,mode);visualMode=mode;visualQuiet=quiet;float a=amount(now,mode,quiet)*CharacterMotionTuning.face();float micro=quiet?0:(ConversationMicroMotion.nod(kind,age(now),sequence)+ConversationMicroMotion.idleNod(idleAge(now,mode,quiet),idleVariant(now)))*CharacterMotionTuning.face()*(mode==2?.35f:1f);return limit(micro+a*(kind==CORRECT?.008f:kind==LISTEN?.006f:kind==JOY?-.006f:.002f)+(quiet?0:performance.nod(now)),.0108f);}
 float lean(long now,int mode,boolean quiet){return amount(now,mode,quiet)*smooth(Math.max(0,now-at-300)/600f)*CharacterMotionTuning.body()*(kind==INVITE?.009f:kind==LISTEN?.005f:kind==EXPLAIN?.003f:0)+touch(now,quiet)*.003f*CharacterMotionTuning.body()+(quiet?0:performance.buttonShoulder(now)+performance.phraseLean(now));}
 float gaze(long now,int mode,boolean quiet){performance.mode(now,mode);visualMode=mode;visualQuiet=quiet;if(quiet)return 0;if(at<0)return performance.buttonGaze(now);float a=amount(now,mode,quiet)*CharacterMotionTuning.eyes();float look=now-at<950?1:-.35f;return a*look*(kind==THINK?-.006f:kind==GUIDE?.006f:.002f)+performance.buttonGaze(now)+(ConversationMicroMotion.gaze(kind,age(now),sequence)+ConversationMicroMotion.idleGaze(idleAge(now,mode,quiet),idleVariant(now)))*CharacterMotionTuning.eyes()*(mode==2?.35f:1f);}
 int face(long now,int mode,boolean quiet){if(amount(now,mode,quiet)<.15f)return -1;return kind==CLARIFY?6:kind==THINK?4:kind==JOY?3:kind==EXPLAIN?5:kind==GUIDE?2:kind==CORRECT?0:kind==INVITE?7:1;}
 long age(long now){return at<0?-1:now-at;}
 boolean moving(long now){return (ConversationMicroMotion.idleAge(now,at,visualMode,blocked,visualQuiet)>=0&&(CharacterMotionTuning.face()>0||CharacterMotionTuning.eyes()>0||CharacterMotionTuning.body()>0))||performance.buttonActive(now)||performance.finishing(now)||at>=0&&now>=at&&now-at<DURATION||touchAt>=0&&now>=touchAt&&now-touchAt<2200;}
 static long invitationPeriod(){return CharacterMotionTuning.intimacy==0?0:CharacterMotionTuning.intimacy==1?360000:CharacterMotionTuning.intimacy==2?180000:90000;}
 static float invitation(long now,int mode,boolean quiet){long period=invitationPeriod();if(period==0||quiet||mode!=0||now<period)return 0;return envelope(now%period-15000,4600);}
 static float held(long age,boolean quiet){if(quiet||age<4000)return 0;return envelope(age%24000-13000,3200)*.36f*CharacterMotionTuning.scale();}
 void deform(float[] mesh,int columns,int rows,int w,int h,long now,int mode,boolean quiet,float faceBottom){
  float lean=lean(now,mode,quiet),tilt=tilt(now,mode,quiet)-(quiet?0:performance.buttonTilt(now)),nod=nod(now,mode,quiet);if(lean==0&&tilt==0&&nod==0)return;
  for(int row=0;row<=rows;row++){
   float y=row/(float)rows;if(y<=faceBottom||y>=.70f)continue;
   float sine=(float)Math.sin(Math.PI*(y-faceBottom)/(.70f-faceBottom)),band=sine*sine;
   for(int col=0;col<=columns;col++){int i=(row*(columns+1)+col)*2;mesh[i]+=w*(lean+tilt*.0015f)*band;mesh[i+1]+=h*nod*band;}
  }
 }
}
