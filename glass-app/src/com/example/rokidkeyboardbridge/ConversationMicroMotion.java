package com.example.rokidkeyboardbridge;
/** Finite local choreography; no new artwork, scaling, timers, random values or IO. */
final class ConversationMicroMotion {
 static float bump(long age,long start,long length){if(age<=start||age>=start+length)return 0;float x=(float)Math.sin(Math.PI*(age-start)/length);return x*x;}
 static float nod(int kind,long age,int variant){if(kind==CharacterGestureLife.LISTEN||kind==CharacterGestureLife.CORRECT)return .0035f*(bump(age,350,1050)+(variant%2==0?.65f:0)*bump(age,1700,1000));if(kind==CharacterGestureLife.JOY)return -.0025f*bump(age,300,1200)+.003f*bump(age,1800,1200);return .0015f*bump(age,900,1700);}
 static float tilt(int kind,long age,int variant){float side=(variant%2==0?-1:1);return (kind==CharacterGestureLife.THINK?-.65f:kind==CharacterGestureLife.LISTEN?.45f:kind==CharacterGestureLife.JOY?.55f:.30f)*side*bump(age,250,3100);}
 static float gaze(int kind,long age,int variant){float side=(variant%2==0?-1:1);return side*.0025f*(bump(age,0,1050)-.4f*bump(age,1800,1200));}
 static long idleAge(long now,long lastEvent,int mode,boolean blocked,boolean quiet){if(blocked||quiet||mode!=0||!CharacterMotionTuning.naturalPoses||lastEvent<0||now<lastEvent||now-lastEvent<14000)return -1;long phase=(now-lastEvent-14000)%28000;return phase<3400?phase:-1;}
 static int idleVariant(long now,long lastEvent){return (int)Math.max(0,(now-lastEvent-14000)/28000)%3;}
 static float idleTilt(long age,int variant){return age<0?0:(variant==0?-.75f:variant==1?.60f:.25f)*bump(age,250,2900);}
 static float idleNod(long age,int variant){return age<0?0:(variant==2?.0025f:.0010f)*bump(age,700,2100);}
 static float idleGaze(long age,int variant){return age<0?0:(variant%2==0?-1:1)*.0035f*(bump(age,0,1100)-.35f*bump(age,1900,1100));}
}
