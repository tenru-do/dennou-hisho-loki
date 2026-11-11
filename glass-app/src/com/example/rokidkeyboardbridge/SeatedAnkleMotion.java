package com.example.rokidkeyboardbridge;
/** Small ankle/toe gestures; upper body and boundary remain rigid. No timer or IO. */
final class SeatedAnkleMotion {
 private long last=-1,age;private boolean allowedBefore;
 long update(long now,boolean allowed){
  if(now<0)return age;
  if(last>=0&&now>=last&&allowed&&allowedBefore)age=(age+Math.min(120,now-last))%36000;
  last=now;allowedBefore=allowed;return age;
 }
 void reset(){last=-1;age=0;allowedBefore=false;}
 static void apply(float[] mesh,int columns,int rows,int width,int height,float[] joints,long age,float gain){
  if(gain<=0||!Float.isFinite(gain))return;gain=Math.min(1,gain);
  float t=(age%36000)/1000f,local=t%12,window=local<4?(float)Math.pow(Math.sin(Math.PI*local/4),2):0;
  int kind=(int)(t/12);float wave=(float)Math.sin(local*Math.PI/2),dx=0,dy=0;
  if(kind==0){dx=2.5f*wave;dy=-1.5f*window;}
  else if(kind==1){dx=-1.6f*wave;dy=-2.3f*window;}
  else{dx=2f*wave;dy=1.8f*wave;}
  float ankleX=joints[4],ankleY=joints[5];
  for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){
   float x=col*362f/columns,y=row*543f/rows;if(y<330||row==rows||col==0||col==columns)continue;
   float nx=(x-ankleX)/55,ny=(y-ankleY)/65,q=Math.max(0,1-nx*nx-ny*ny);q=q*q*window*gain;
   int at=(row*(columns+1)+col)*2;mesh[at]+=width/362f*dx*q;mesh[at+1]+=height/543f*dy*q;
  }
 }
}
