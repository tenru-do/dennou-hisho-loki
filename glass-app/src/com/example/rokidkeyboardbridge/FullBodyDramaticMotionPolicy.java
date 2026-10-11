package com.example.rokidkeyboardbridge;
/** Three real breath poses and two real arm intermediates; no unrelated posture rotation. */
final class FullBodyDramaticMotionPolicy {
 static final long STRESS_CYCLE_MS=12000L,HOLD_MS=5400L,BRIDGE_MS=600L;
 static long period(int effects){return (effects&FullBodyDramaticConversationPolicy.ROUGH_BREATH)!=0?900L:1800L;}
 private static long phase(long elapsed,long period){return Math.max(0,elapsed)%period;}
 static int breathFrom(long elapsed,long period){int step=(int)(phase(elapsed,period)*4/period);return step==0?0:step==1?1:step==2?2:1;}
 static int breathTo(long elapsed,long period){int step=(int)(phase(elapsed,period)*4/period);return step==0?1:step==1?2:step==2?1:0;}
 static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
 static float breathBlend(long elapsed,long period){long quarter=period/4;return ease((phase(elapsed,period)%quarter)/(float)quarter);}
 static boolean bridge(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);return (t>=HOLD_MS&&t<HOLD_MS+BRIDGE_MS)||t>=2*HOLD_MS+BRIDGE_MS;}
 static boolean second(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);return t>=HOLD_MS+BRIDGE_MS&&t<2*HOLD_MS+BRIDGE_MS;}
 static long holdElapsed(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);return second(elapsed)?t-HOLD_MS-BRIDGE_MS:t;}
 static int bridgeFrom(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);boolean back=t>=2*HOLD_MS+BRIDGE_MS;int step=(int)((back?t-2*HOLD_MS-BRIDGE_MS:t-HOLD_MS)/200);return back?(step==0?3:step==1?7:6):(step==0?0:step==1?6:7);}
 static int bridgeTo(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);boolean back=t>=2*HOLD_MS+BRIDGE_MS;int step=(int)((back?t-2*HOLD_MS-BRIDGE_MS:t-HOLD_MS)/200);return back?(step==0?7:step==1?6:0):(step==0?6:step==1?7:3);}
 static float bridgeBlend(long elapsed){long t=phase(elapsed,STRESS_CYCLE_MS);long local=t>=2*HOLD_MS+BRIDGE_MS?t-2*HOLD_MS-BRIDGE_MS:t-HOLD_MS;return ease((local%200)/200f);}
}
