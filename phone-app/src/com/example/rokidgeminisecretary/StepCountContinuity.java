package com.example.rokidgeminisecretary;
final class StepCountContinuity {
 static long estimate(long health,long anchor,long total,long base,boolean sameDay){
  if(sameDay&&health>=0)return health+(anchor>=0&&total>=anchor?total-anchor:0);
  return Math.max(0,total-base);
 }
}
