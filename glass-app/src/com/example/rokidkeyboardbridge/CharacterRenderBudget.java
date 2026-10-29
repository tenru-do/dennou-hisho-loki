package com.example.rokidkeyboardbridge;
/** Render-cost feedback with hysteresis. Drops detail, never pose or input. */
final class CharacterRenderBudget {
 static volatile int quality;private static int slow,good;private static long changed=-1;
 static synchronized void sample(long now,long nanos){if(nanos<0)return;long ms=nanos/1000000;if(ms>45){good=0;if(++slow>=3){quality=Math.min(2,quality+1);changed=now;slow=0;}}else{slow=0;if(ms<18&&++good>=24&&quality>0&&changed>=0&&now>=changed&&now-changed>=10000){quality--;changed=now;good=0;}}}
 static synchronized void pressure(long now){quality=2;changed=now;slow=good=0;}
 static boolean rich(){return quality==0;}static long delay(long base){return quality==2?Math.max(125,base):quality==1?Math.max(66,base):base;}
 static synchronized void reset(){quality=slow=good=0;changed=-1;}
 private CharacterRenderBudget(){}
}
