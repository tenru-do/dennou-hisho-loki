package com.example.rokidkeyboardbridge;
/** Audited mouth landmarks in the existing 384x512 overhead cells; body and head scale stay fixed. */
final class LyingFaceMotion {
 private static final int[][] MOUTH={{254,79},{210,79},{182,91},{139,90},{252,79},{211,74},{183,90},{148,73}};
 static int x(int clip,int frame,int width){return MOUTH[(clip-28)*4+frame][0]*width/384;}
 static int y(int clip,int frame,int height){return MOUTH[(clip-28)*4+frame][1]*height/512;}
 static void apply(float[] mesh,int columns,int rows,int w,int h,float mouthX,float mouthY,int expression,long age){float gain=(expression==1||expression==2)?Math.min(1,Math.max(0,age)/450f):0;int n=0;for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){float x=w*col/(float)columns,y=h*row/(float)rows,dx=(x-mouthX)/10f,dy=(y-mouthY)/6f,weight=Math.max(0,1-dx*dx-dy*dy);mesh[n++]=x;mesh[n++]=y-1.25f*gain*weight*Math.min(1,Math.abs(dx)*2);}}
}
