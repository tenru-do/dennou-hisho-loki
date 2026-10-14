package com.example.rokidkeyboardbridge;
/** Chest-only displacement: eyes/head and floor stay fixed; reusable mesh storage. */
final class FullBodyBreathPolicy {
 static final int COLUMNS=16,ROWS=32;
 static void laugh(float[] vertices,int width,int height,long elapsed){
  laugh(vertices,width,height,elapsed,.15f);
 }
 static void laugh(float[] vertices,int width,int height,long elapsed,float faceBottom){
  resting(vertices,width,height,elapsed,faceBottom);
  float pulse=(float)Math.sin(Math.max(0,elapsed)*2*Math.PI/360L);
  for(int row=0;row<=ROWS;row++){
   float ratio=row/(float)ROWS;
   float start=Math.max(.18f,faceBottom+.065f),end=Math.min(.85f,start+.32f);
   float band=ratio<=start||ratio>=end?0:(float)Math.pow(Math.sin(Math.PI*(ratio-start)/(end-start)),2);
   for(int col=0;col<=COLUMNS;col++)vertices[(row*(COLUMNS+1)+col)*2+1]+=height*.003f*band*pulse;
  }
 }
 static void resting(float[] vertices,int width,int height,long elapsed,float faceBottom){
  float begin=Math.max(.18f,Math.min(.75f,faceBottom+.065f));
  float end=Math.min(.82f,begin+.36f);
  double t=Math.max(0,elapsed);
  float breath=(float)((1-Math.cos(t*2*Math.PI/5200))/2);
  float settle=(float)Math.sin(t*2*Math.PI/9100);
  float sway=(float)Math.sin(t*2*Math.PI/11800);
  float inertia=(float)Math.sin((t-320)*2*Math.PI/11800);
  float fabric=(float)Math.sin((t-620)*2*Math.PI/11800);
  int at=0;
  for(int row=0;row<=ROWS;row++){
   float ratio=row/(float)ROWS;
   float band=ratio<=begin||ratio>=end?0:(float)Math.pow(Math.sin(Math.PI*(ratio-begin)/(end-begin)),2);
   float torso=ratio<=begin||ratio>=.88f?0:(float)Math.pow(Math.sin(Math.PI*(ratio-begin)/(.88f-begin)),2);
   float hip=ratio<=.42f||ratio>=.78f?0:(float)Math.pow(Math.sin(Math.PI*(ratio-.42f)/.36f),2);
   float hem=ratio<=.53f||ratio>=.85f?0:(float)Math.pow(Math.sin(Math.PI*(ratio-.53f)/.32f),2);
   for(int col=0;col<=COLUMNS;col++){
    float x=width*col/(float)COLUMNS;
    float edge=(float)Math.pow(Math.abs(col/(float)COLUMNS-.5f)*2,2);
    vertices[at++]=x+(x-width*.5f)*.024f*band*breath+width*.005f*band*settle+width*.014f*torso*sway+width*.002f*(band+hip)*inertia+width*.004f*hem*edge*fabric;
    vertices[at++]=height*ratio-height*.0095f*band*breath+height*.0008f*(band+hip)*inertia+height*.0012f*hem*edge*fabric;
    float protect=Math.max(0,Math.min(1,(ratio-faceBottom-.045f)/.10f));protect=protect*protect*(3-2*protect);
    vertices[at-2]=x+(vertices[at-2]-x)*protect;vertices[at-1]=height*ratio+(vertices[at-1]-height*ratio)*protect;
    vertices[at-2]=x+Math.max(-width*.034f,Math.min(width*.034f,vertices[at-2]-x));
   }
  }
 }
 static void mesh(float[] vertices,int width,int height,long elapsed){
  float breath=(float)((1-Math.cos(Math.max(0,elapsed)%5200L*2*Math.PI/5200))/2);
  int at=0;
  for(int row=0;row<=ROWS;row++){
   float ratio=row/(float)ROWS,y=height*ratio;
   float band=ratio<=.18f||ratio>=.50f?0:(float)Math.sin(Math.PI*(ratio-.18f)/.32f);
   band*=band;
   for(int column=0;column<=COLUMNS;column++){
    float x=width*column/(float)COLUMNS;
    vertices[at++]=x+(x-width*.5f)*.012f*band*breath;
    vertices[at++]=y-height*.0025f*band*breath;
   }
  }
 }
}
