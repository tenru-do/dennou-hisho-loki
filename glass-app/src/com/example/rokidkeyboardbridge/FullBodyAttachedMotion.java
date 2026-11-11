package com.example.rokidkeyboardbridge;
/** User attachments retain their original artwork; all movement uses source coordinates. */
final class FullBodyAttachedMotion {
 static final int FIRST_SCENE=20,COUNT=4;
 static final String[] ASSETS={"loki_crouch_strain_v488.png","loki_balance_fall_v488.png","loki_overhead_stretch_v488.png","loki_low_braced_v489.png"};
 static final float[][] FEATURES={
 {480f,708f,144.74731592f,95.47388285f,14.59564596f,0.23240124f,4.60534940f,0.00000000f,184.91577002f,97.30042440f,17.89507252f,-0.32020726f,5.63900515f,0.00000000f,160.01558004f,142.93483760f,30.47753332f,6.67926695f,18.57711216f},
 {1039f,1514f,424.34096937f,280.85577720f,22.30879184f,-10.91003326f,9.68148103f,1.00000000f,475.46194812f,264.78973061f,22.31153810f,-26.72714977f,10.23023175f,1.00000000f,466.56102590f,317.02668893f,27.33310000f,-19.19767255f,13.87682465f},
 {792f,1986f,368.06025129f,380.49231127f,25.53788280f,11.12812047f,0.64084847f,0.00000000f,421.93974871f,395.50768873f,22.30857932f,19.42924924f,1.92491088f,0.00000000f,380.81759978f,436.63779462f,31.21865842f,12.48626822f,5.55475291f},
 {479f,1024f,235.86930602f,166.59013713f,13.17438711f,11.35907688f,0.46804027f,0.00000000f,263.34727723f,173.66190755f,11.26391401f,16.71359567f,0.96323540f,0.00000000f,242.69369556f,193.29819543f,17.38541396f,11.72472512f,3.68731955f}
};
 static final float[][] BOUNDS={{91f,427f,34f,677f},{23f,1001f,70f,1436f},{212f,560f,24f,1895f},{23.00000000f,451.00000000f,121.00000000f,933.00000000f}};
 static int group(int scene){return scene>=FIRST_SCENE&&scene<FIRST_SCENE+COUNT?scene-FIRST_SCENE:-1;}
 static int effects(int scene){int g=group(scene);return g==0?5:(g==1||g==3)?1:0;}
 static float scale(int id,int width,int height){float[] f=FEATURES[id],b=BOUNDS[id];float eye=(float)Math.hypot(f[8]-f[2],f[9]-f[3]);return FullBodySceneScalePolicy.reference(width,height)/Math.max(1,eye);}
 static float left(int id,int width,float scale){float[] f=FEATURES[id],b=BOUNDS[id];float center=(f[2]+f[8])*.5f;return width*.44f-center*scale;}
 static float top(int id,int height,float scale){return CharacterViewport.top(height-6-BOUNDS[id][3]*scale,BOUNDS[id][2],scale,height);}
 static float rock(int id,long elapsed){double t=Math.max(0,elapsed);return(float)(Math.sin(t*Math.PI*2/(id==1?3500:6100))*(id==1?.45:.24));}
}
