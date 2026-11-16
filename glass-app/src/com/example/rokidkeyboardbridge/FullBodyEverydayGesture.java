package com.example.rokidkeyboardbridge;
/** Four same-posture clips. The ear habit occurs once per two-minute idle cycle. */
final class FullBodyEverydayGesture {
 // Frame 3 is the lowered-hand return, not another waving position.
 static final int[] GREETING={0,4,5,6,1,2,1,2,1,7,4,3},EAR={0,1,2,3,0},OTHER={0,1,2,1,0};
 static final long[] GREETING_MS={300,180,180,180,460,400,400,400,400,220,220,600},EAR_MS={500,650,1100,650,700};
 static boolean allowed(int mode,boolean held,FullBodyPoseCatalog.Kind kind){
  return mode==0&&(!held||kind==null||kind==FullBodyPoseCatalog.Kind.NEUTRAL||kind==FullBodyPoseCatalog.Kind.LISTEN||kind==FullBodyPoseCatalog.Kind.THINK||kind==FullBodyPoseCatalog.Kind.JOY||kind==FullBodyPoseCatalog.Kind.EXPLAIN||kind==FullBodyPoseCatalog.Kind.CONFIDENT);
 }
 static int clip(boolean seated,boolean ear){return clip(seated,ear?1:0);}
 static int clip(boolean seated,int family){return family*2+(seated?1:0);}
 static long period(){return CharacterMotionTuning.frequency==0?180000:CharacterMotionTuning.frequency==2?80000:120000;}
 static long variationOffset(long elapsed){return CharacterMotionTuning.variant(Math.max(0,elapsed)/period())*700L;}
 static int automatic(long elapsed,boolean seated,boolean thinking){
  long t=Math.max(0,elapsed)%period()-variationOffset(elapsed);
  if(!seated&&t>=period()/4&&t<period()/4+4600)return clip(false,2);
  if(!seated&&t>=period()*3/4&&t<period()*3/4+4600)return clip(false,3);
  if(t>=50000&&t<54600)return clip(seated,true);
  if(!thinking&&t>=12000&&t<16600)return clip(seated,false);
  return -1;
 }
 static long local(long elapsed,int clip){long t=Math.max(0,elapsed)%period()-variationOffset(elapsed);return Math.max(0,t-(clip==4?period()/4:clip==6?period()*3/4:clip>=2?50000:12000));}
 static int frame(int clip,long elapsed){if(clip>=8)return KissGesturePolicy.bodyFrame((clip-8)/2,elapsed);int[] sequence=clip==2||clip==3||clip==7?EAR:clip>=2?OTHER:GREETING;long[] durations=clip>=2?EAR_MS:GREETING_MS;long t=Math.max(0,elapsed);for(int i=0;i<sequence.length;i++){if(t<durations[i])return sequence[i];t-=durations[i];}return 0;}
}
