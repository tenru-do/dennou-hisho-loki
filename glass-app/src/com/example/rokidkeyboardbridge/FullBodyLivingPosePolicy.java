package com.example.rokidkeyboardbridge;
/** Local upper-body cycles keep the face/contact hand and floor aligned. */
final class FullBodyLivingPosePolicy {
 static int profile(int pose,float[] f){
  pose=FullBodyClothMotion.base(pose);
  if(pose>=0&&pose<168){String g=FullBodyPoseCatalog.ALL[pose].gesture;
   if(g.contains("chin"))return 2;
   if(g.contains("arms")&&(g.contains("crossed")||g.contains("fold")))return 3;
   if(!FullBodyMotionPolicy.isSeated(pose)&&f!=null&&f[7]<.5f&&f[13]<.5f)return 1;
  }return 0;
 }
 static void apply(float[] v,int w,int h,long elapsed,float face,int kind){
  double t=Math.max(0,elapsed);
  float inhale=(float)Math.sin(t*2*Math.PI/4400);
  float shoulder=(float)Math.sin(t*2*Math.PI/6100);
  float elbow=(float)Math.sin((t-430)*2*Math.PI/5300);
  float shift=(float)Math.sin(t*2*Math.PI/9700);
  float begin=Math.max(.08f,face+.008f);
  float gain=kind==2?1.55f:kind==1?1.7f:kind==3?1.5f:1;
  for(int row=0;row<=FullBodyBreathPolicy.ROWS;row++){
   float y=row/(float)FullBodyBreathPolicy.ROWS;
   float neck=band(y,begin,Math.min(.70f,begin+.34f));
   float arm=band(y,begin+.065f,Math.min(.80f,begin+.47f));
   float body=band(y,begin,.90f);
   float chest=band(y,begin+.018f,Math.min(.70f,begin+.32f));
   float expansion=(1-inhale)*.5f;
   for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++){
    float x=col/(float)FullBodyBreathPolicy.COLUMNS;
    float side=(x-.5f)*2,outer=side*side;
    int a=(row*(FullBodyBreathPolicy.COLUMNS+1)+col)*2;
    v[a]+=w*gain*(.010f*neck*shoulder+.010f*arm*side*elbow+.007f*body*shift);
    v[a+1]+=h*gain*(-.0035f*neck*inhale+.0045f*neck*outer*shoulder+.0025f*arm*side*elbow);
    if(kind==3){v[a]+=w*.022f*(x-.5f)*chest*expansion;v[a+1]-=h*.006f*chest*expansion;}
   }
  }
 }
 private static float band(float y,float from,float to){
  if(y<=from||y>=to)return 0;
  float s=(float)Math.sin(Math.PI*(y-from)/(to-from));return s*s;
 }
}
