package com.example.rokidkeyboardbridge;
/** Bounded rigid face/chin-hand translation plus shoulder breathing. No timers or allocations. */
final class SeatedPresenceMotion {
 static void sample(float[] out,long now,float gain,boolean speaking,int emotion,float accepted){
  double t=Math.max(0,now);gain=Math.max(0,Math.min(1,gain));
  float breathe=(float)Math.sin(t*2*Math.PI/5200);
  float listening=QuietPosePerformance.pulse((long)(t%14500)-4500,2400);
  float think=emotion==QuietPosePerformance.THOUGHT?1f:.35f;
  out[0]=gain*(.0018f*(float)Math.sin(t*2*Math.PI/11700)*think);
  out[1]=gain*(.0020f*listening+.0015f*accepted+(speaking?.0010f*(float)Math.sin(t*2*Math.PI/1900):0));
  out[2]=gain*.0022f*(float)Math.sin(t*2*Math.PI/7100);
  out[3]=gain*.0015f*breathe;
 }
 static float upper(float y,float faceBottom){float start=Math.max(.24f,faceBottom+.07f),end=Math.min(.48f,start+.16f);if(y<=start)return 1;if(y>=end)return 0;float a=(y-start)/(end-start);return 1-a*a*(3-2*a);}
 static void fill(float[] mesh,int columns,int rows,int width,int height,float faceBottom,float[] motion){
  int at=0;for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){
   float x=col/(float)columns,y=row/(float)rows,upper=upper(y,faceBottom),chest=Math.max(0,1-Math.abs(y-(faceBottom+.16f))/.16f);
   float edge=Math.min(1,Math.min(x,1-x)/.20f);if(col==0||col==columns||row==0||row==rows)edge=0;
   mesh[at++]=width*(x+edge*(motion[0]*upper+motion[2]*chest*(1-upper)));
   mesh[at++]=height*(y+edge*(motion[1]*upper+motion[3]*chest*(1-upper)));
  }
 }
 private SeatedPresenceMotion(){}
}
