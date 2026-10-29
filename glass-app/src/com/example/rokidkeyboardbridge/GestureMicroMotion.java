package com.example.rokidkeyboardbridge;
/** Small cloth/chest breathing; head, outer chair edges and floor stay anchored.
 * Mutates the caller's reusable mesh only. No bitmap, timer, IO or allocation. */
final class GestureMicroMotion {
 static void apply(float[] mesh,int columns,int rows,float width,float height,float headX,float top,float bottom,long now,float gain){
  if(gain<=0||bottom<=top||columns<=0||rows<=0)return;
  gain=Math.min(1.25f,gain);float span=bottom-top;
  float breath=(float)Math.sin(Math.floorMod(now,4400L)*Math.PI*2/4400.0);
  for(int row=0;row<=rows;row++){
   float y=height*row/rows,v=(y-top)/span;if(v<=.17f||v>=.48f)continue;
   float band=(float)Math.sin(Math.PI*(v-.17f)/.31f);band*=band;
   for(int col=0;col<=columns;col++){
   float x=width*col/columns,u=(x-headX)/(width*.28f);if(Math.abs(u)>=1)continue;
   float center=1-u*u;center*=center;int i=(row*(columns+1)+col)*2;
   mesh[i]+=width*.0025f*u*center*band*breath*gain;
   mesh[i+1]-=span*.0032f*center*band*breath*gain;
   }
  }
 }
 private GestureMicroMotion(){}
}
