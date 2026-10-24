package com.example.rokidkeyboardbridge;
/** After a gap/bad fix require two distinct, fresh samples; no coordinates retained. */
public final class NavigationPositionRecovery {
 private String route="";private long last,first;private boolean waiting;
 public synchronized boolean accept(String identity,long now,long sample,double accuracy,double maxAccuracy){if(!identity.equals(route)){route=identity;last=first=0;waiting=false;}boolean valid=sample>0&&now>=sample&&now-sample<=30000&&Double.isFinite(accuracy)&&accuracy>=0&&accuracy<=maxAccuracy;if(!valid){waiting=true;first=0;return false;}if(last>0&&sample-last>15000){waiting=true;first=0;}if(sample<last)return false;if(sample==last)return !waiting;last=sample;if(!waiting)return true;if(first==0){first=sample;return false;}if(sample-first<1000)return false;waiting=false;first=0;return true;}
 public synchronized boolean waiting(){return waiting;}
}
