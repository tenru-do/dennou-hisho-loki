package com.example.rokidkeyboardbridge;
/** Same mature identity for six seated families. Pose scale remains 0.76 of standing. */
final class SeatedGestureAtlas {
 static final String ASSET="loki_seated_gestures_v687.png";
 static final String EAR="loki_seated_ear_v689.png";
 static final String BECKON="loki_seated_beckon_v689.png";
 static String asset(int clip){return clip==3?EAR:clip==7?BECKON:ASSET;}
 static boolean supports(int clip){return clip==1||clip==3||clip==5||clip==7||clip==23||clip==25;}
 static int[] bounds(int clip,int frame,int width,int height){if(!supports(clip)||frame<0||frame>3)throw new IllegalArgumentException("seated atlas");if(clip==3||clip==7)return new int[]{frame%2*width/2,frame/2*height/2,(frame%2+1)*width/2,(frame/2+1)*height/2};int row=clip==23?4:clip==25?5:clip/2,column=frame;if(clip==23){if(frame==0)column=1;if(frame==1){row=2;column=2;}}return new int[]{column*width/4,row*height/6+(row>0?Math.max(1,height/768):0),(column+1)*width/4,(row+1)*height/6};}
}
