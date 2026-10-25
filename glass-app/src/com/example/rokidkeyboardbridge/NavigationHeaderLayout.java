package com.example.rokidkeyboardbridge;
/** Pixel budgets for the fixed-size glasses display, including half-screen modes. */
final class NavigationHeaderLayout {
 static int mapHeight(int requested,int viewport,int header,int controls,int gap){
  if(viewport<=0)return Math.max(1,requested);
  return Math.max(1,Math.min(requested,viewport-Math.max(0,header)-Math.max(0,controls)-Math.max(0,gap)));
 }
 static int textHeight(int lineHeight,int lines,int fontTop,int ascent,int descent,int fontBottom){
  return Math.max(1,lineHeight*Math.max(1,lines)+Math.max(0,ascent-fontTop)+Math.max(0,fontBottom-descent));
 }
 private NavigationHeaderLayout(){}
}
