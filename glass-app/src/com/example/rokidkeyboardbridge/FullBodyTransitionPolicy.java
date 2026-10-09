package com.example.rokidkeyboardbridge;
final class FullBodyTransitionPolicy {
 static final long FRAME_MS=105, DURATION_MS=FRAME_MS*6;
 static final float[] POSTURE_HEIGHT={.76f,.78f,.82f,.90f,.97f,1f};
 static int kind(int from,int to){if(from<0||from==to)return 0;if(FullBodyMotionPolicy.isSeated(from)!=FullBodyMotionPolicy.isSeated(to))return 1;if((from==4&&(to==0||to==14))||(to==4&&(from==0||from==14)))return 2;return 0;}
 static boolean reverse(int kind,int from,int to){return kind==1?FullBodyMotionPolicy.isSeated(to):to==4;}
 static int frame(long elapsed,boolean reverse){int n=(int)Math.min(5,Math.max(0,elapsed)/FRAME_MS);return reverse?5-n:n;}
 static float height(int kind,int frame){return kind==1?POSTURE_HEIGHT[frame]:1f;}
}
