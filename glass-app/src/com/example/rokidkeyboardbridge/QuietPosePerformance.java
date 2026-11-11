package com.example.rokidkeyboardbridge;
/** Same-posture local expression and feedback. No network, timer or success inference. */
final class QuietPosePerformance {
 static final int CALM=0,THOUGHT=1,JOY=2,CONCERN=3,RELIEF=4;
 private int emotion,oldEmotion,mode,phase=-1,scene,region=CharacterContactPoint.SHOULDER,direction,variant;private long receivedAt=-1,readyAt=-1,lastReceive=-10000,emotionAt,calmSince=-1;private boolean touch;
 void accepted(long now,boolean touching){accepted(now,touching,CharacterContactPoint.SHOULDER,0);}
 void accepted(long now,boolean touching,int point,int swipe){if(now<lastReceive||now-lastReceive<500)return;lastReceive=receivedAt=now;touch=touching;region=Math.max(0,Math.min(2,point));direction=swipe;variant=(variant+1)%3;}
 void update(int nextMode,int nextPhase,int nextScene,FullBodyPoseCatalog.Kind kind,long now){
  // A new spoken response is ready, not proof that an external operation succeeded.
  if(mode!=2&&nextMode==2&&phase>=0)readyAt=now;
  mode=nextMode;phase=nextPhase;scene=nextScene;
  int next=kind==FullBodyPoseCatalog.Kind.THINK||nextPhase==CharacterChoreography.THINK?THOUGHT:kind==FullBodyPoseCatalog.Kind.JOY?JOY:kind==FullBodyPoseCatalog.Kind.WORRY||kind==FullBodyPoseCatalog.Kind.SAD?CONCERN:kind==FullBodyPoseCatalog.Kind.RECOVERY?RELIEF:CALM;
  if(next==CALM&&emotion!=CALM){if(calmSince<0)calmSince=now;if(now-calmSince<1200)return;}else calmSince=-1;
  if(next!=emotion){oldEmotion=emotion;emotion=next;emotionAt=now;}
 }
 int emotion(){return emotion;}
 int expression(long now){return emotion==CALM&&now>=emotionAt&&now-emotionAt<650?oldEmotion:emotion;}
 float expressionGain(long now){return emotion==CALM?Math.max(0,1-Math.max(0,now-emotionAt)/650f):1;}
 int variant(){return variant;}
 static float pulse(long age,long duration){return age<0||age>=duration?0:(float)Math.sin(Math.PI*age/duration);}
 float gain(){return scene==1?0:scene==2?.35f:1f;}
 float acceptance(long now){return gain()*pulse(now-receivedAt,touch?950:650);}
 float ready(long now){return gain()*pulse(now-readyAt,1300);}
 float nod(long now){return acceptance(now)*(touch&&region==CharacterContactPoint.HAND?.12f:variant==0?.35f:variant==1?.55f:.43f)+ready(now)*.22f;}
 float hand(long now){return touch&&region==CharacterContactPoint.HAND?acceptance(now):0;}
 float shoulder(long now){return touch&&region==CharacterContactPoint.SHOULDER?acceptance(now):0;}
 float gazeX(long now){float e=expression(now)==THOUGHT?-.32f:expression(now)==CONCERN?.12f:(float)Math.sin(now/13000.0)*.10f;return e*(emotion==CALM?expressionGain(now):1)+(touch?(direction==1?.24f:direction==2?-.24f:0)*acceptance(now):0);}
 float gazeY(){return emotion==THOUGHT?.32f:emotion==CONCERN?.18f:emotion==RELIEF?.1f:0;}
 boolean active(long now){return gain()>0&&(acceptance(now)>0||ready(now)>0||now>=emotionAt&&now-emotionAt<650);}
 boolean mayChange(long now,boolean manual){return manual||mode==0&&scene!=1&&(receivedAt<0||now-receivedAt>=8000)&&(readyAt<0||now-readyAt>=8000);}
 static long cycleAge(long age,int scene){return Math.max(0,age)/(scene==2?2:1);}
 static int transitionStage(long age){return age<0?0:age<240?0:age<800?1:age<1250?2:3;}
 static float transitionBlend(long age){if(age<240)return 0;if(age>=800)return 1;float x=(age-240)/560f;return x*x*(3-2*x);}
 static float prepare(long age){return age<0||age>=240?0:pulse(age,480);}
 static float settle(long age){return age<800||age>=1250?0:pulse(age-800,450);}
 static boolean samePosture(int a,int b){return ApprovedQuietPosePolicy.valid(a)&&ApprovedQuietPosePolicy.valid(b)&&ApprovedQuietPosePolicy.seatedIndex(a)==ApprovedQuietPosePolicy.seatedIndex(b);}
}
