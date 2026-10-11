package com.example.rokidkeyboardbridge;
/** Chest-only displacement: eyes/head and floor stay fixed; reusable mesh storage. */
final class FullBodyBreathPolicy {
 static final int COLUMNS=4,ROWS=32;
 static void laugh(float[] vertices,int width,int height,long elapsed){
  mesh(vertices,width,height,elapsed);
  float pulse=(float)Math.sin(Math.max(0,elapsed)*2*Math.PI/360L);
  for(int row=0;row<=ROWS;row++){
   float ratio=row/(float)ROWS;
   float band=ratio<=.18f||ratio>=.50f?0:(float)Math.pow(Math.sin(Math.PI*(ratio-.18f)/.32f),2);
   for(int col=0;col<=COLUMNS;col++)vertices[(row*(COLUMNS+1)+col)*2+1]+=height*.003f*band*pulse;
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
