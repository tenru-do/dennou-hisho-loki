package com.example.rokidkeyboardbridge;
/** Brief embarrassment/nervous fidget. No body-size change, clothing change or idle trigger. */
final class ShyFidgetMotion {
 static final int TYPE=8;static final long DURATION=4800;
 static boolean request(String text){String t=EmbodiedScenePolicy.clean(text);return PoisedMotionPolicy.valid(t)&&!EmbodiedScenePolicy.has(t,"しないで","しない","やめて","ないで","小説","物語","例文")&&EmbodiedScenePolicy.has(t,"身もだえして","身もだえて","身もだえる仕草","身悶えして","身悶えて","照れて身をよじって","照れて体をよじって","緊張してもじもじして","もじもじして");}
 static boolean active(long at,long now){return at>=0&&now>=at&&now-at<DURATION;}
 static float envelope(long elapsed){if(elapsed<=0||elapsed>=DURATION)return 0;float t=elapsed/(float)DURATION;return (float)Math.sin(Math.PI*t);}
 static int face(long elapsed){return elapsed<650||elapsed>=3800?0:2;}
 static float headAngle(long elapsed){return 2.2f*(float)Math.sin(elapsed/520.0)*envelope(elapsed);}
 static void apply(float[] points,int cols,int rows,float width,float height,float headX,float headBottom,float headWidth,float headHeight,long elapsed){float e=envelope(elapsed);if(e==0)return;float twist=(float)Math.sin(elapsed/480.0)*e,shoulder=(float)Math.sin(elapsed/330.0)*e;
 for(int row=0;row<=rows;row++)for(int col=0;col<=cols;col++){float x=width*col/cols,y=height*row/rows,tx=(x-headX)/Math.max(1,headWidth*1.25f),ty=(y-(headBottom+headHeight*.55f))/Math.max(1,headHeight*.9f);float torso=Math.max(0,1-Math.abs(tx))*Math.max(0,1-Math.abs(ty));float hand=Math.max(0,1-Math.abs((x-(headX+headWidth*.08f))/Math.max(1,headWidth*.5f)))*Math.max(0,1-Math.abs((y-(headBottom+headHeight*1.35f))/Math.max(1,headHeight*.35f)));int i=(row*(cols+1)+col)*2;points[i]+=headWidth*.035f*twist*torso+headWidth*.012f*shoulder*hand;points[i+1]-=headHeight*.018f*shoulder*torso+headHeight*.009f*twist*hand;}
 }
}
