package com.example.rokidkeyboardbridge;
final class QuickControlPolicy {
 static int brightness(int percent){return Math.max(5,Math.min(100,percent));}
 static String swipe(float dx,float dy,float threshold){
  if(Math.max(Math.abs(dx),Math.abs(dy))<threshold)return "";
  // Horizontal swipes never dispatch music or adjust brightness.
  if(Math.abs(dy)>Math.abs(dx)*1.3f)return dy<0?"brighter":"dimmer";
  return "";
 }
}
