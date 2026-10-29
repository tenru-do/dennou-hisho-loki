package com.example.rokidkeyboardbridge;
/** Shared visual preference. No per-frame storage access. */
final class CharacterMotionTuning {
 private static final int[] ORDER={0,2,1,3,1,0,3,2};
 static volatile boolean naturalPoses=true;
 static volatile int eyeLevel=2,faceLevel=2,bodyLevel=2,approachLevel=2;
 static float part(int value){return value==0?0:value==1?.5f:1f;}
 static float eyes(){return part(eyeLevel);}static float face(){return part(faceLevel);}static float body(){return part(bodyLevel);}static float approach(){return part(approachLevel);}
 static void parts(int eyes,int face,int body,int approach){eyeLevel=Math.max(0,Math.min(2,eyes));faceLevel=Math.max(0,Math.min(2,face));bodyLevel=Math.max(0,Math.min(2,body));approachLevel=Math.max(0,Math.min(2,approach));}
 static volatile float intentGain=1,intentSpeed=1;
 static long time(long age){return (long)(Math.max(0,age)*intentSpeed);}
 static volatile int level=1;
 static volatile int intimacy=1;
 static void intimacy(int value){intimacy=Math.max(0,Math.min(3,value));}
 static float scale(){return (level==0?.45f:level==2?1.25f:1f)*CharacterChoreography.gain*intentGain;}
 static void set(int value){level=Math.max(0,Math.min(2,value));}
 static int variant(long count){return ORDER[(int)(Math.max(0,count)%ORDER.length)];}
 private CharacterMotionTuning(){}
}
