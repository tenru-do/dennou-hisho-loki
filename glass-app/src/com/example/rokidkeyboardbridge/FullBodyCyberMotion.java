package com.example.rokidkeyboardbridge;
/** Six-frame adjacent loops. Dedicated scene IDs never alias the legacy stress cycle. */
final class FullBodyCyberMotion {
 static final int FIRST=365,GROUPS=13,FRAMES=6;
 static int group(int scene){return scene>=7&&scene<7+GROUPS?scene-7:-1;}
 static int groupForPose(int pose){return pose>=FIRST&&pose<FIRST+GROUPS*FRAMES?(pose-FIRST)/FRAMES:-1;}
 static int effects(int scene){int g=group(scene);if(g<0)return 0;return 8|1|(g==11||g==12?0:4)|(g==2?2:0);}
 static float position(long elapsed){double t=(Math.max(0,elapsed)%7200L)/1200.0;int node=(int)t;double f=t-node;return Math.min(Math.nextDown((float)(node+1)),(float)(node+f*f*f*(f*(f*6-15)+10)));}
 static void rough(float[] mesh,int w,int h,long elapsed,float faceBottom){
  float begin=Math.max(.18f,Math.min(.55f,faceBottom+.025f)),end=Math.min(.86f,begin+.30f);
  double pulse=Math.sin(Math.max(0,elapsed)*Math.PI*2/1400.0);
  for(int row=0;row<=FullBodyBreathPolicy.ROWS;row++){
   float y=row/(float)FullBodyBreathPolicy.ROWS;
   double band=y<=begin||y>=end?0:Math.pow(Math.sin(Math.PI*(y-begin)/(end-begin)),2);
   for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++)mesh[(row*(FullBodyBreathPolicy.COLUMNS+1)+col)*2+1]+=(float)(h*.006*band*pulse);
  }
 }
 static float scale(int pose,int bw,int bh,int width,int height){
  int g=groupForPose(pose),first=FIRST+g*FRAMES;float raw=FullBodySceneScalePolicy.reference(width,height)/FullBodySceneScalePolicy.CYBER[g];
  for(int i=first;i<first+FRAMES;i++)raw=Math.min(raw,Math.min((width-6f)/(FullBodyAnchorPolicy.MAX[i]-FullBodyAnchorPolicy.MIN[i]),height/FullBodyRealisticFeatures.ALL[i][1]));
  return raw*FullBodyRealisticFeatures.ALL[pose][0]/bw;
 }
}
