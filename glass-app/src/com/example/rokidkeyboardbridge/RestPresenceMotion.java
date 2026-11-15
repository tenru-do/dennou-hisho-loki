package com.example.rokidkeyboardbridge;
/** Local breathing, forearm settling and ankle movement. No IO, sensor, timer or allocation. */
final class RestPresenceMotion {
 static float bump(float x,float y,float cx,float cy,float rx,float ry){float dx=(x-cx)/rx,dy=(y-cy)/ry;float q=Math.max(0,1-dx*dx-dy*dy);return q*q;}
 static boolean active(long now){return now>=0&&now%14000<3400;}
 static void apply(float[] mesh,int cols,int rows,int width,int height,float faceBottom,long now,float gain,boolean seated){
  gain=Math.max(0,Math.min(1,gain));double t=Math.max(0,now);float breath=(float)Math.sin(t*2*Math.PI/5600),settle=active(now)?(float)Math.sin((t%14000)*Math.PI/3400):0;
  for(int row=0;row<=rows;row++)for(int col=0;col<=cols;col++){
   float x=col/(float)cols,y=row/(float)rows;if(row==0||row==rows||col==0||col==cols||y<faceBottom+.035f)continue;
   float chest=bump(x,y,.5f,Math.min(.42f,faceBottom+.14f),.27f,.14f),arm=bump(x,y,seated?.53f:.60f,seated?.40f:.34f,.19f,.12f),ankle=bump(x,y,.56f,.82f,.22f,.12f);int at=(row*(cols+1)+col)*2;
   mesh[at]+=width*gain*(.0038f*settle*arm+.0042f*settle*ankle);
   mesh[at+1]+=height*gain*(-.0028f*breath*chest+.0025f*settle*arm);
  }
 }
 private RestPresenceMotion(){}
}
