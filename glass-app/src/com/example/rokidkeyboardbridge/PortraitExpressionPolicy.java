package com.example.rokidkeyboardbridge;
/** A calm idle sequence and bounded acknowledgements; negative semantic faces win. */
final class PortraitExpressionPolicy {
 static final int[] IDLE={0,1,5,0,4,1,3,0};
 static boolean supports(int expression){return expression==0||expression==1||expression==14;}
 static int cell(int expression,int mode,long now,long cueAt,int cue,boolean quiet){
  if(!supports(expression))return -1;
  if(quiet)return mode==1?1:0;
  if(cueAt>=0&&now>=cueAt&&now-cueAt<1600)return Math.max(0,Math.min(7,cue));
  if(mode==1)return (Math.max(0,now)/5000)%3==2?4:1;
  if(mode==2)return (Math.max(0,now)/3600)%2==0?0:5;
  return IDLE[(int)(Math.max(0,now)/8000%IDLE.length)];
 }
 static long delay(int mode,boolean quiet){return quiet?(mode==2?125:300):(mode==0?125:80);}
}
