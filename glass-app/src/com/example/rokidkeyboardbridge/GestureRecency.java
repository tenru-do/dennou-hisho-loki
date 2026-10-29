package com.example.rokidkeyboardbridge;
/** Per-view bounded history. Repeated redraws continue one gesture, not new repetitions. */
final class GestureRecency {
 private final long[] started=new long[22];private final boolean[] seen=new boolean[22];private int active=-1;private long activeAt;
 boolean allow(int clip,long now,boolean explicit){if(clip<0||clip>43)return false;int family=clip/2;if(!explicit&&family==active&&now>=activeAt&&now-activeAt<6000)return true;
 if(!explicit&&seen[family]&&now>=started[family]&&now-started[family]<120000)return false;
 active=family;activeAt=now;started[family]=now;seen[family]=true;return true;}
 void note(int clip,long now){allow(clip,now,true);}
}
