package com.example.rokidkeyboardbridge;
/** Reviewed standing-only replacement; seated clips retain their dedicated artwork. */
final class StandingGestureAtlas {
 static boolean supports(int clip){return clip==0||clip==2||clip==4||clip==6||clip==22||clip==24;}
 static String asset(int clip){return clip==2||clip==24?"loki_standing_accessory_v685.png":"loki_standing_gestures_v685.png";}
 static float headCenter(int clip,int frame,int width){
  int column=clip==22?new int[]{1,2,3,0}[frame]:frame;
  float[] centers=clip==2||clip==24?new float[]{250/1536f,615/1536f,980/1536f,1345/1536f}:new float[]{176/1086f,418/1086f,660/1086f,903/1086f};
  return centers[column]*width-column*width/4;
 }
 static void protectHead(float[] mesh,int columns,int rows,float width,float height,float top,float bottom){
  float span=Math.max(1,bottom-top);
  for(int row=0;row<=rows;row++){float y=height*row/rows,v=(y-top)/span,gain=Math.max(0,Math.min(1,(v-.19f)/.08f));if(gain>=1)continue;
   for(int col=0;col<=columns;col++){int i=(row*(columns+1)+col)*2;float x=width*col/columns;mesh[i]=x+(mesh[i]-x)*gain;mesh[i+1]=y+(mesh[i+1]-y)*gain;}
  }
 }
 static int[] bounds(int clip,int frame,int width,int height){
  if(!supports(clip)||frame<0||frame>3)throw new IllegalArgumentException("standing atlas region");
  int rows=clip==2||clip==24?2:4;
  int row=clip==0?3:clip==4?1:clip==6?2:clip==24?1:0;
  int column=clip==22?new int[]{1,2,3,0}[frame]:frame;
  return new int[]{column*width/4,row*height/rows,(column+1)*width/4,(row+1)*height/rows};
 }
}
