package com.example.rokidkeyboardbridge;
/** Subtle local ankle pivot and long hanging cloth settling on the approved draped artwork. */
final class DrapedSeatedMotion {
 static void apply(float[] mesh,int columns,int rows,int width,int height,int pose,long now,float gain){
  if(pose<0||pose>2||gain<=0)return;gain=Math.min(1,gain);
  double t=Math.max(0,now);float envelope=QuietPosePerformance.pulse((long)(t%26000)-5000,12000);
  float angle=.018f*envelope*(float)Math.sin(t*2*Math.PI/8000)*gain;
  float cloth=.004f*envelope*(float)Math.sin((t-450)*2*Math.PI/6300)*gain;
  float kneeX=pose==1?.72f:.69f,kneeY=pose==1?.41f:.44f;
  for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){
   float x=col/(float)columns,y=row/(float)rows;if(row==rows||col==0||col==columns||y<.40f)continue;
   float leg=Math.max(0,1-Math.abs(x-(.69f+(y-.44f)*.36f))/.13f)*Math.max(0,1-Math.abs(y-.67f)/.27f);
   float skirt=Math.max(0,1-Math.abs(x-.47f)/.15f)*Math.max(0,1-Math.abs(y-.75f)/.22f);
   int at=(row*(columns+1)+col)*2;
   // Small rotation around the knee rather than changing the calf's scale.
   float px=(x-kneeX)*width,py=(y-kneeY)*height;
   float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
   mesh[at]+=(px*c-py*s-px)*leg+width*cloth*skirt;
   mesh[at+1]+=(px*s+py*c-py)*leg;
  }
 }
 private DrapedSeatedMotion(){}
}
