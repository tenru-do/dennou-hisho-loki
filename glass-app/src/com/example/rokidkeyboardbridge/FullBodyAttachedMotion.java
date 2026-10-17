package com.example.rokidkeyboardbridge;
/** User attachments retain their original artwork; all movement uses source coordinates. */
final class FullBodyAttachedMotion {
 static final int FIRST_SCENE=20,COUNT=4;
 static final String[] ASSETS={"loki_crouch_strain_v488.png","loki_balance_fall_v488.png","loki_overhead_stretch_v488.png","loki_low_braced_v489.png"};
 static final float[][] FEATURES={{480.00000000f,708.00000000f,155.88000298f,111.82068332f,13.20993800f,0.23240124f,4.16811837f,0.00000000f,192.23487139f,113.47381335f,16.19611761f,-0.32020726f,5.10363903f,0.00000000f,169.69870090f,154.77570370f,27.58400188f,6.67926695f,16.81340453f},{1039.00000000f,1514.00000000f,413.79107438f,296.42858601f,28.45120184f,-10.91003326f,12.34713977f,1.00000000f,478.98749089f,275.93898094f,28.45470424f,-26.72714977f,13.04698123f,1.00000000f,467.63582575f,342.55864620f,34.85888212f,-19.19767255f,17.69761186f},{792.00000000f,1986.00000000f,382.87594606f,369.82150915f,25.53788280f,11.12812047f,0.64084847f,0.00000000f,436.75544348f,384.83688661f,22.30857932f,19.42924924f,1.92491088f,0.00000000f,395.63329455f,425.96699250f,31.21865842f,12.48626822f,5.55475291f},{479.00000000f,1024.00000000f,232.15713352f,193.09423009f,13.90996794f,11.35907688f,0.49417291f,0.00000000f,261.16931415f,200.56084687f,11.89282518f,16.71359567f,1.01701684f,0.00000000f,239.36255711f,221.29351038f,18.35611394f,11.72472512f,3.89319794f}};
 static final float[][] BOUNDS={{91f,427f,34f,677f},{23f,1001f,70f,1436f},{212f,560f,24f,1895f},{23.00000000f,451.00000000f,121.00000000f,933.00000000f}};
 static int group(int scene){return scene>=FIRST_SCENE&&scene<FIRST_SCENE+COUNT?scene-FIRST_SCENE:-1;}
 static int effects(int scene){int g=group(scene);return g==0?5:(g==1||g==3)?1:0;}
 static float scale(int id,int width,int height){float[] f=FEATURES[id],b=BOUNDS[id];float eye=(float)Math.hypot(f[8]-f[2],f[9]-f[3]);return Math.min(FullBodySceneScalePolicy.reference(width,height)/Math.max(1,eye),Math.min((width-14f)/(b[1]-b[0]),(height-12f)/(b[3]-b[2])));}
 static float left(int id,int width,float scale){float[] f=FEATURES[id],b=BOUNDS[id];float center=(f[2]+f[8])*.5f;return Math.max(7-b[0]*scale,Math.min(width-7-b[1]*scale,width*.44f-center*scale));}
 static float top(int id,int height,float scale){return height-6-BOUNDS[id][3]*scale;}
 static float rock(int id,long elapsed){double t=Math.max(0,elapsed);return(float)(Math.sin(t*Math.PI*2/(id==1?3500:6100))*(id==1?.45:.24));}
}
