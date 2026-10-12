package com.example.rokidkeyboardbridge;
public final class FullBodyAllLivingMotionTest{
 static int images,samples;
 static void check(float[]f){float face=(f[15]+Math.max(f[16]*.25f,f[18])+4)/f[1];float[]v=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2],old=v.clone();boolean moving=false;
  for(long t=0;t<=18200;t+=33){FullBodyBreathPolicy.resting(v,240,598,t,face);
   for(int row=0;row<=FullBodyBreathPolicy.ROWS;row++)for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++){int at=(row*(FullBodyBreathPolicy.COLUMNS+1)+col)*2;float x=240*col/(float)FullBodyBreathPolicy.COLUMNS,y=598*row/(float)FullBodyBreathPolicy.ROWS;
    if(!Float.isFinite(v[at])||!Float.isFinite(v[at+1]))throw new AssertionError("Invalid mesh");
    if((row/(float)FullBodyBreathPolicy.ROWS<=face||row==FullBodyBreathPolicy.ROWS)&&(Math.abs(v[at]-x)>.001||Math.abs(v[at+1]-y)>.001))throw new AssertionError("Face/floor moves image "+images);
    if(t>0&&(Math.abs(v[at]-old[at])>.15||Math.abs(v[at+1]-old[at+1])>.15))throw new AssertionError("Motion jump");
    if(Math.abs(v[at]-x)+Math.abs(v[at+1]-y)>.8)moving=true;
   }System.arraycopy(v,0,old,0,v.length);samples++;
  }if(!moving)throw new AssertionError("Frozen image "+images);images++;
 }
 public static void main(String[]args){
  for(float[]f:FullBodyRealisticFeatures.ALL)check(f);
  for(float[]f:FullBodyApprovedFeatures.ALL)check(f);
  for(float[][]group:FullBodyDramaticFeatures.ALL)for(float[]f:group)check(f);
  for(int i=0;i<6;i++){float[]b=FullBodyRearHoldPolicy.BOUNDS[i];float[]f=new float[19];f[1]=i<3?720:816;f[15]=b[2]+(b[3]-b[2])*.20f-4;check(f);}
  System.out.println("PASS all "+images+" individual image meshes, "+samples+" time samples, fixed face/floor and continuous visible-range motion");
 }
}
