package com.example.rokidkeyboardbridge;
public final class FullBodyRestingMotionTest {
 public static void main(String[]args){
  float[] v=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
  float[] previous=v.clone();int cases=0;
  for(float face:new float[]{.15f,.23f,.40f}){
   boolean moving=false;
   for(long time=0;time<=18200;time+=33){
    FullBodyBreathPolicy.resting(v,300,720,time,face);
    for(int row=0;row<=FullBodyBreathPolicy.ROWS;row++)for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++){
     int i=(row*(FullBodyBreathPolicy.COLUMNS+1)+col)*2;float x=300*col/(float)FullBodyBreathPolicy.COLUMNS,y=720*row/(float)FullBodyBreathPolicy.ROWS;
     if(!Float.isFinite(v[i])||!Float.isFinite(v[i+1]))throw new AssertionError("Invalid vertex");
     if(row/(float)FullBodyBreathPolicy.ROWS<=face||row==FullBodyBreathPolicy.ROWS){if(Math.abs(v[i]-x)>.001f||Math.abs(v[i+1]-y)>.001f)throw new AssertionError("Face or feet drift");}
     if(Math.abs(v[i]-x)>10.5||Math.abs(v[i+1]-y)>8.2f)throw new AssertionError("Resting amplitude excessive x="+Math.abs(v[i]-x)+" y="+Math.abs(v[i+1]-y));
     if(time>0&&(Math.abs(v[i]-previous[i])>.25f||Math.abs(v[i+1]-previous[i+1])>.25f))throw new AssertionError("Resting motion discontinuity");
     if(Math.abs(v[i]-x)+Math.abs(v[i+1]-y)>.8f)moving=true;
    }
    System.arraycopy(v,0,previous,0,v.length);cases++;
   }
   if(!moving)throw new AssertionError("Frozen resting pose");
  }
  System.out.println("PASS resting motion: "+cases+" standing/seated face-band timeline checks; face/feet fixed, visible body movement, bounded continuous motion");
 }
}
