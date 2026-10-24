package com.example.rokidkeyboardbridge;
/** Consume a complete select gesture; repeated/down-up duplicates cannot toggle twice. */
public final class SpeechQuickKey {
 private int down;private long last=-1;
 public static boolean select(int key){return key==23||key==109;}
 public synchronized int event(int key,int action,int repeat,boolean available,long now){if(!select(key))return 0;int bit=key==23?1:2;if(action==1){if((down&bit)==0)return 0;down&=~bit;return 2;}if(action!=0)return 0;if((down&bit)!=0)return 2;if(!available)return 0;down|=bit;if(repeat>0||last>=0&&now>=last&&now-last<400)return 2;last=now;return 1;}
 public synchronized void clear(){down=0;}
}
