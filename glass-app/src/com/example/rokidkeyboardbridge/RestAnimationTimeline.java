package com.example.rokidkeyboardbridge;
/** Finite keyframe motion followed by the same corrected neutral body. No continuous auto-loop. */
final class RestAnimationTimeline {
 static final int MASTER=9;
 private static final int[][] KEYS={{9},{9,1,2,1,9},{9,4,5,9},{9,7,8,9},{9,10,11,12,11,13,9},{9,14,15,16},{16,17,14,9},{16}};
 private static final int[][] TIMES={{0},{0,800,2000,3500,4800,6000},{0,900,2700,4200,5400},{0,800,2300,3800,5000},{0,600,1200,2000,2600,3200,4000,4800},{0,600,1400,2600,4200},{0,800,1600,2400,3600},{0,4800}};
 static int phase(int motion,long age){if(motion<1||motion>7)return 0;int p=0;for(int i=1;i<KEYS[motion].length;i++)if(age>=TIMES[motion][i])p=i;return p;}
 static int frame(int motion,long age){if(motion<1||motion>7)return MASTER;return KEYS[motion][phase(motion,Math.max(0,age))];}
 static int previous(int motion,long age){if(motion<1||motion>7)return MASTER;return KEYS[motion][Math.max(0,phase(motion,age)-1)];}
 static float mix(int motion,long age){if(motion<1||motion>7)return 1;int p=phase(motion,age);if(p==0)return 1;float v=Math.max(0,Math.min(1,(age-TIMES[motion][p])/420f));return v*v*(3-2*v);}
 static boolean moving(int motion,long age){return motion>=1&&motion<=7&&age>=0&&age<TIMES[motion][TIMES[motion].length-1];}
 private RestAnimationTimeline(){}
}
