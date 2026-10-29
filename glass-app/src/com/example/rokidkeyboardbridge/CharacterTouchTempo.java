package com.example.rokidkeyboardbridge;
final class CharacterTouchTempo {
 private static long at=-1,duration;
 static void note(long now,long elapsed){at=now;duration=Math.max(0,Math.min(2000,elapsed));}
 static long duration(long now){return at>=0&&now>=at&&now-at<200?duration:0;}
 private CharacterTouchTempo(){}
}
