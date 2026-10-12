package com.example.rokidkeyboardbridge;
/** Shared content bounds per approved motion, normalized by source height. */
final class FullBodyApprovedScalePolicy {
 static final float[][] BOUNDS={{0.15722656f,0.49707031f,0.03723057f,0.94726562f},{0.13997396f,0.61197917f,0.01432292f,0.97460938f},{0.12475506f,0.55388635f,0.01892950f,0.94973890f}};
 static int group(int face){return face<3?0:face<6?1:2;}
 static float height(int face,int width,int height,float fraction){float[] b=BOUNDS[group(face)];if(group(face)!=1)return Math.min(FullBodySceneScalePolicy.reference(width,height)/FullBodySceneScalePolicy.APPROVED[group(face)],Math.min(height*fraction/(b[3]-b[2]),Math.max(1,width-2)/(b[1]-b[0])));return Math.min(height*fraction/(b[3]-b[2]),Math.max(1,width-2)/(b[1]-b[0]));}
 static float left(int face,int width,float h){float[] b=BOUNDS[group(face)];return width*.5f-h*(b[0]+b[1])*.5f;}
 static float top(int face,int height,float h){return height-h*BOUNDS[group(face)][3];}
}
