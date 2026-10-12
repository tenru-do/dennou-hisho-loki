package com.example.rokidkeyboardbridge;
/** Accessory changes wait for a compatible quiet pose, and pause for conversation. */
final class FullBodyAccessoryTimeline {
 static final long DURATION=2450;
 boolean wearing,target;private long start=-1,last;
 boolean moving(){return start>=0;}
 boolean wants(long now){return (Math.max(0,now)%480000>=390000)!=wearing;}
 long elapsed(long now){return Math.max(0,now-start);}
 int pose(int node,int base){return node==0||node>=7?base:329+(target?node-1:6-node);}
 void update(long now,int mode,boolean blending,boolean ready){
  long delta=Math.max(0,now-last);last=now;
  if(moving()){
   if(mode!=0||blending||!ready){start+=delta;return;}
   if(elapsed(now)>=DURATION){wearing=target;start=-1;}
   return;
  }
  if(mode==0&&!blending&&ready&&wants(now)){target=!wearing;start=now;}
 }
}
