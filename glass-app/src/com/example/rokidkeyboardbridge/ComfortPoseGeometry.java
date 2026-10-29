package com.example.rokidkeyboardbridge;
/** Manually inspected inner-face crops; body always comes from the neutral panel. */
final class ComfortPoseGeometry {
 static final String[] ASSETS={"loki_comfort_hug_v678.png","loki_comfort_read_v678.png","loki_comfort_side_v678.png","loki_comfort_stretch_v678.png","loki_offduty_pillow_v683.png","loki_offduty_arms_v683.png","loki_offduty_chin_v683.png","loki_offduty_dream_v683.png","loki_rest_relaxed_legs_v684.png"};
 static final int[][] FACE={{224,92,92,100},{358,358,78,92},{139,70,78,102},{207,64,82,94},{228,46,63,83},{229,45,63,83},{245,47,63,84},{229,47,63,84},{228,46,63,83}};
 static final int[] HEAD_WIDTH={163,112,136,113,94,94,94,94,94};
 static int width(int pose){return 512;}static int height(int pose){return 768;}
 static int top(int pose){return 0;}static int bottom(int pose){return 768;}
 static float originY(int pose,int height,float scale){return CharacterViewport.top(height*.96f-768*scale,Math.max(0,FACE[pose][1]-FACE[pose][3]*.8f),scale,height);}
 static float scale(int pose,int width,int height){return CharacterViewport.calibratedHead(HEAD_WIDTH[pose],width,height);}
 static long duration(int pose){return pose==7?5200:2800;}
 static float entry(int pose,long age){if(pose<4)return envelope(age);long duration=duration(pose);if(age<=0||age>=duration)return 0;float fade=(float)Math.sin(Math.PI*age/duration);return fade*fade*(.65f+.35f*(float)Math.sin(age/130.0));}
 static int expression(int face){return face==5||face==6?1:face==7?2:face==2||face==3||face==4||face==8||face==9?3:0;}
 static int expression(int pose,int face){if(pose<4)return expression(face);return face==10?3:face==5||face==6?1:face==7||face==2||face==3||face==4||face==8||face==9?2:0;}
 static int[] crop(int pose,int variant){int[] f=FACE[pose];int x=f[0],y=f[1];if(pose==1&&variant>=2)y-=52;if(pose==2&&variant>=2)y-=4;return new int[]{(variant%2)*width(pose)+x,(variant/2)*height(pose)+y,f[2],f[3]};}
 static float envelope(long age){if(age<=0||age>=2800)return 0;float v=(float)Math.sin(Math.PI*age/2800f);return v*v;}
 static void animate(float[] mesh,int cols,int rows,int pose,long age,int reaction,long reactionAge,float gain){int[] f=FACE[pose];float e=Math.max(entry(pose,age),RestInteractionMotion.envelope(reactionAge)*(reaction>0?1:0))*Math.max(0,Math.min(1,gain));if(e==0)return;float cx=f[0]+f[2]*.5f,cy=f[1]+f[3]+48;for(int row=0;row<=rows;row++)for(int col=0;col<=cols;col++){float x=width(pose)*(float)col/cols,y=height(pose)*(float)row/rows,weight=Math.max(0,1-Math.abs(x-cx)/115f)*Math.max(0,1-Math.abs(y-cy)/90f);int k=(row*(cols+1)+col)*2;mesh[k]+=(pose>=4?3.2f*(float)Math.sin(age/160.0):1.7f)*e*weight*(pose==3?1:-1);mesh[k+1]+=(pose>=4?1.6f:.9f)*e*weight;}}
 private ComfortPoseGeometry(){}
}
