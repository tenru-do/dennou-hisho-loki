package com.example.rokidkeyboardbridge;
/** Subpixel breathing inside the chest only; never moves face, waving arm, lap or legs. */
final class GreetingPresenceMotion {
 static void fill(float[] mesh,int cols,int rows,float width,float height,float headX,float top,float bottom,long now,float gain){
  for(int r=0;r<=rows;r++)for(int c=0;c<=cols;c++){int i=(r*(cols+1)+c)*2;mesh[i]=width*c/cols;mesh[i+1]=height*r/rows;}
  apply(mesh,cols,rows,width,height,headX,top,bottom,now,gain);
 }
 static void apply(float[] mesh,int cols,int rows,float width,float height,float headX,float top,float bottom,long now,float gain){
  if(gain<=0||bottom<=top)return;gain=Math.min(1,gain);float span=bottom-top;
  float breath=(float)Math.sin(Math.floorMod(now,5600L)*2*Math.PI/5600);
  for(int r=1;r<rows;r++)for(int c=1;c<cols;c++){
   float x=width*c/cols,y=height*r/rows,v=(y-top)/span,u=(x-headX)/(width*.13f);
   if(v<=.20f||v>=.36f||Math.abs(u)>=1)continue;
   float b=(float)Math.sin(Math.PI*(v-.20f)/.16f);b*=b;float lateral=1-u*u;lateral*=lateral;
   mesh[(r*(cols+1)+c)*2+1]-=span*.0018f*b*lateral*breath*gain;
  }
 }
 private GreetingPresenceMotion(){}
}
