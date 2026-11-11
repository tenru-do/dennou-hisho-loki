package com.example.rokidkeyboardbridge;
/** EV selection is applied by the sensor before JPEG encoding, never a destructive filter. */
final class KeepExposurePolicy {
 static int mode(int value){return value < -1 || value > 1 ? 0 : value;}
 static String label(int value){return value<0?"日向・白飛びを抑える（−1EV）":value>0?"夜道・明るめ（＋1EV）":"自動（日向・暗所を判定）";}
 static int compensation(int mode,float step,int minimum,int maximum){
  if(!Float.isFinite(step)||step<=0||minimum>maximum)return 0;
  return Math.max(minimum,Math.min(maximum,Math.round(mode(mode)/step)));
 }
 private KeepExposurePolicy(){}
}
