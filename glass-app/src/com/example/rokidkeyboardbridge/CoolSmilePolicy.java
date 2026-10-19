package com.example.rokidkeyboardbridge;
/** Cool overrides exclude seated frames: retain their original continuous head/neck anatomy. */
final class CoolSmilePolicy {
 static boolean supports(int pose){return pose==1||pose==5||pose==18||(pose>=305&&pose<=328)||(pose>=347&&pose<=352);}
 static float[] features(int pose,float[] original){float[] calibrated=CoolSmileFeatures.forPose(pose);return calibrated==null?original:calibrated;}
 static boolean protectsTransition(int from,int to){return supports(from)||supports(to);}
 static boolean portrait(int cell){return cell==1||cell==5||cell==12;}
 static float mouthEyeAngle(){return (float)(5-Math.toDegrees(Math.atan2(24,150)));}

 static int seatedPose(FullBodyPoseCatalog.Kind kind,boolean seated,int otherwise){return seated&&kind==FullBodyPoseCatalog.Kind.JOY?168:otherwise;}
}
