package com.example.rokidkeyboardbridge;
/** Six quiet breathing frames after the one-shot refusal; no repeated push. */
final class FullBodyRearHoldPolicy{
 static final String ASSET="loki_rear_hold_breath_6_v472.png";
 static final float[][] BOUNDS={{23.000000f,328.000000f,12.000000f,695.000000f,155.502050f},{13.000000f,315.000000f,12.000000f,695.000000f,144.142431f},{20.000000f,326.000000f,12.000000f,695.000000f,151.502082f},{23.000000f,332.000000f,21.000000f,719.000000f,153.446243f},{11.000000f,318.000000f,21.000000f,718.000000f,142.665299f},{17.000000f,326.000000f,21.000000f,718.000000f,147.969593f}};
 static final int ROW_BREAK=720;
 static final long START=1910L,PERIOD=5200L;
 static int from(long time){return (int)(Math.max(0,time)%PERIOD*6/PERIOD);}
 static int to(long time){return (from(time)+1)%6;}
 static float blend(long time){float f=(Math.max(0,time)%PERIOD*6f/PERIOD)%1;return f*f*(3-2*f);}
 static float bodyHeight(int width,int height){float[] b=FullBodyApprovedScalePolicy.BOUNDS[1];float h=FullBodyApprovedScalePolicy.height(3,width,height,1)*(b[3]-b[2]);for(float[] f:BOUNDS)h=Math.min(h,Math.max(1,width-2)*(f[3]-f[2])/(f[1]-f[0]));return h;}
}
