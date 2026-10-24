package com.example.rokidkeyboardbridge;
/** Monotonic hold and deduplication; never queues a stale maneuver for replay. */
public final class NavigationVoiceSession {
 private long holdUntil,lastAt;private String lastKey="",lastText="";
 public synchronized void interrupt(long now){holdUntil=Math.max(holdUntil,now+2500);}
 public synchronized boolean held(long now,boolean speaking,boolean answering){return speaking||answering||now<holdUntil;}
 public synchronized boolean admit(String key,String text,long now,boolean explicit){if(!explicit&&key.equals(lastKey)&&text.equals(lastText)&&now>=lastAt&&now-lastAt<20000)return false;lastKey=key;lastText=text;lastAt=now;return true;}
 private NavigationVoiceSession(boolean unused){} public NavigationVoiceSession(){}
}
