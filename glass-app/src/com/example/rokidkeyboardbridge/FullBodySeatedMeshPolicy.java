package com.example.rokidkeyboardbridge;
/** Keep head, neck and shoulder attachment fixed; retain bounded breathing below it. */
final class FullBodySeatedMeshPolicy {
 static float weight(float y,float faceBottom){
  float start=Math.max(.22f,Math.min(.70f,faceBottom+.16f));
  float t=Math.max(0,Math.min(1,(y-start)/.18f));return t*t*(3-2*t);
 }
 static void stabilize(float[] v,int columns,int rows,int w,int h,float faceBottom,boolean normalized){
  int at=0;
  for(int r=0;r<=rows;r++)for(int c=0;c<=columns;c++){
   float y=r/(float)rows,x=c/(float)columns,keep=weight(y,faceBottom);
   float baseX=normalized?0:w*x,baseY=normalized?0:h*y;
   float boundX=normalized?.008f:w*.004f,boundY=normalized?.012f:h*.007f;
   v[at]=baseX+Math.max(-boundX,Math.min(boundX,v[at]-baseX))*keep;
   v[at+1]=baseY+Math.max(-boundY,Math.min(boundY,v[at+1]-baseY))*keep;at+=2;
  }
 }
}
