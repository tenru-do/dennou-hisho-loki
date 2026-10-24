package com.example.rokidkeyboardbridge;
/** One pending directional cue. Urgent cues cannot be replaced by a lower-priority cue. */
public final class NavigationSpeechLane {
 private String key="",message="";private long token,at;private boolean pending,urgent;
 public synchronized long offer(String key,String message,boolean urgent,long now,boolean explicit){if(!explicit&&pending&&this.key.equals(key)&&now>=at&&now-at<=10000&&(this.message.equals(message)||this.urgent&&!urgent))return 0;this.key=key;this.message=message;this.urgent=urgent;this.at=now;pending=true;return ++token;}
 public synchronized boolean valid(long token,String key,long now){return pending&&token==this.token&&this.key.equals(key)&&now>=at&&now-at<=10000;}
 public synchronized boolean urgentPending(long now){return pending&&urgent&&now>=at&&now-at<=10000;}
 public synchronized void complete(long token){if(this.token==token)pending=false;}
 public synchronized void clear(){pending=false;token++;key=message="";}
}
