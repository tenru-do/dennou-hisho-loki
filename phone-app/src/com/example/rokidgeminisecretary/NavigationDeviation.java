package com.example.rokidgeminisecretary;
/** Distinct, fresh location samples only; HUD polling cannot manufacture a deviation. */
public final class NavigationDeviation {
 private String identity="";private long first,last;private int count;private boolean outside;
 public synchronized boolean observe(String route,String mode,long now,long sample,double accuracy,double speed,double metres){
  if(!route.equals(identity)){identity=route;reset();}
  boolean walking="walking".equals(mode),cycling="bicycling".equals(mode);
  double limit=walking?40:cycling?60:100;
  if(!Double.isFinite(accuracy)||accuracy<0||accuracy>limit||sample<=0||now<sample||now-sample>30000||!Double.isFinite(metres)||metres<0){reset();return false;}
  if(sample<=last)return outside&&count>=3&&last-first>=8000;
  if(last>0&&sample-last>15000)reset();
  double threshold=Math.max(walking?45:cycling?80:150,accuracy*2.5);
  if(Double.isFinite(speed)&&speed>=12)threshold=Math.max(threshold,800);
  last=sample;
  if(metres<=threshold){first=0;count=0;outside=false;return false;}
  outside=true;if(first==0)first=sample;count++;return count>=3&&sample-first>=8000;
 }
 public static boolean validFix(long now,long sample,double accuracy){return sample>0&&now>=sample&&now-sample<=30000&&Double.isFinite(accuracy)&&accuracy>=0&&accuracy<=100;}
 public synchronized String status(){return outside?"経路からの離れを確認中（GPSの揺れを確認）":"";}
 public synchronized void reset(){first=last=0;count=0;outside=false;}
}
