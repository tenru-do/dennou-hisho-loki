package com.example.rokidkeyboardbridge;
/** Event-relative listening/thinking/answering gestures; no timer, sensor or allocations. */
final class PortraitAttentionMotion {
 private int mode=-1;private long changed;
 void mode(int next,long now){if(mode!=next||now<changed){mode=next;changed=now;}}
 float age(long now){return Math.max(0,now-changed);}
 float gaze(long now,boolean quiet){if(quiet)return 0;float age=age(now);if(mode==1)return -.010f*envelope(age,180,2000);if(mode==2)return .004f*envelope(age,0,650);return 0;}
 float tilt(long now,boolean quiet){if(quiet)return 0;float age=age(now);return mode==1?-2.2f*envelope(age,250,2400):mode==2?1.2f*envelope(age,0,900):0;}
 float nod(long now,boolean quiet){return quiet||mode!=2?0:.009f*envelope(age(now),0,700);}
 private static float envelope(float age,float start,float duration){if(age<=start||age>=start+duration)return 0;float s=(float)Math.sin(Math.PI*(age-start)/duration);return s*s;}
}
