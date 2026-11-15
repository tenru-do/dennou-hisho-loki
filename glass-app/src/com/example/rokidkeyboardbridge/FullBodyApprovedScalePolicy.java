package com.example.rokidkeyboardbridge;
/** Shared content bounds per approved motion, normalized by source height. */
final class FullBodyApprovedScalePolicy {
 static final float[][] BOUNDS={{0.15722656f,0.49707031f,0.03723057f,0.94726562f},{0.13997396f,0.61197917f,0.01432292f,0.97460938f},{0.12475506f,0.55388635f,0.01892950f,0.94973890f}};
 static int group(int face){return face<3?0:face<6?1:2;}
 static float eyeSpan(int face){
  float[] f=FullBodyApprovedFeatures.ALL[face];
  // Profile frames deliberately store the same coordinate for the hidden eye.
  // Use the visible eye's reviewed width, never the zero inter-eye distance.
  return group(face)==1?f[4]*2.45f:CharacterViewport.eyes(f);
 }
 static float height(int face,int width,int height,float fraction){float[] f=FullBodyApprovedFeatures.ALL[face];return CharacterViewport.eyeScale(CharacterDistanceCalibration.APPROVED[face],width,height)*f[1];}

 static float left(int face,int width,float h){float[] b=BOUNDS[group(face)];return width*.5f-h*(b[0]+b[1])*.5f;}
 static float top(int face,int height,float h){return CharacterViewport.headMargin(height)-BOUNDS[group(face)][2]*h;}
}
