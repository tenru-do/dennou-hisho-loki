package com.example.rokidkeyboardbridge;
final class FullBodyClothMotion {
 static final int FIRST=341;private static final int[] BASE={4,1,2,168};
 static int group(int pose){return pose==4?0:pose==1?1:pose==2?2:pose==168?3:-1;}
 static int base(int pose){return pose>=FIRST&&pose<FIRST+24?BASE[(pose-FIRST)/6]:pose;}
 static int pose(int group,int node){
  if(group==3){if(node<=0||node>=12)return 168;return FIRST+18+(node<=6?node-1:11-node);}
  return node<=0||node>=7?BASE[group]:FIRST+group*6+node-1;
 }
 static float position(long elapsed){return position(elapsed,0);}
 static float position(long elapsed,int group){
  float x;if(group==3){long t=Math.max(0,elapsed)%120000L;x=t<12000?t/2000f:t<36000?6:t<48000?6+(t-36000)/2000f:0;}
  else x=Math.max(0,elapsed)%8400L/1200f;
  int n=(int)x;double t=x-n;t=t*t*t*(t*(t*6-15)+10);return Math.min(Math.nextDown(group==3?12f:7f),(float)(n+t));
 }
}
