package com.example.rokidkeyboardbridge;
/** Only explicit vendor notifications establish wear state. Wearing never resumes audio. */
public final class WearPauseState {
 private int state=-1;
 public synchronized boolean accept(String raw){if(!"0".equals(raw)&&!"1".equals(raw))return false;int next="1".equals(raw)?1:0;boolean changed=next!=state;state=next;return changed;}
 public synchronized boolean removed(){return state==0;}
 public synchronized String label(){return state<0?"装着通知：未取得・この端末での利用は未確認":state==0?"取り外し検知：読み上げ停止・再開は装着後に操作":"装着検知：再開は決定タップ";}
}
