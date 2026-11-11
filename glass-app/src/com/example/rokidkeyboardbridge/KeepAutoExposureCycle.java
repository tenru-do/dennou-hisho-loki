package com.example.rokidkeyboardbridge;
/** At most one analysis/correction per photo, then at most two settling frames. */
final class KeepAutoExposureCycle {
 private boolean decided;private long appliedAt=-1;private int settlingFrames;
 boolean needsAnalysis(){return !decided;}
 boolean correcting(){return appliedAt>=0;}
 float decide(KeepAutoExposurePolicy.Stats stats,long exposureNs,int iso){if(decided)return 0;decided=true;return KeepAutoExposurePolicy.ev(stats,exposureNs,iso);}
 void applied(long now){if(appliedAt<0)appliedAt=now;}
 boolean discardSettling(long now){if(appliedAt<0)return false;if(now<appliedAt||now-appliedAt>=900)return false;return settlingFrames++<2;}
}
