package com.example.rokidkeyboardbridge;
import android.graphics.*;
/** Localized monochrome face effects. Reuses drawing objects; no body or clothing overlays. */
final class FullBodySituationEffects {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path drop=new Path();
 void draw(Canvas canvas,Bitmap source,float[] f,long elapsed,int effects){
  if(source==null||f==null||effects==0)return;
  int saved=canvas.save();canvas.scale(source.getWidth()/f[0],source.getHeight()/f[1]);
  float width=Math.max(f[4],f[10]);if((effects&FullBodyDramaticConversationPolicy.CYBER_HIGHLIGHT)!=0)width*=1.3f;if(width<=0){canvas.restoreToCount(saved);return;}
  if((effects&FullBodyDramaticConversationPolicy.SWEAT)!=0){
   float t=(Math.max(0,elapsed)%2800)/2800f;
   int side=f[10]>0?8:2;bead(canvas,f[side]+width*.75f,f[side+1]-width*.6f+t*width*.55f,width*.13f,Math.min(1,Math.min(t*6,(1-t)*5)));
  }
  if((effects&FullBodyDramaticConversationPolicy.TEARS)!=0){
   for(int eye=0;eye<2;eye++){
    int offset=eye==0?2:8;if(f[offset+2]<=0||(eye==1&&Math.abs(f[8]-f[2])<width*.4f))continue;float t=((Math.max(0,elapsed)+(eye==0?0:750))%2400)/2400f;
    float x=f[offset]+(eye==0?-1:1)*f[offset+2]*.25f,y=f[offset+1]+width*.15f+t*width*1.3f;
    bead(canvas,x,y,width*.10f,Math.min(1,Math.min(t*8,(1-t)*6)));
   }
  }
  canvas.restoreToCount(saved);
 }
 private void bead(Canvas c,float x,float y,float radius,float alpha){
  if(alpha<=0)return;
  drop.reset();drop.moveTo(x,y-radius*2);drop.cubicTo(x-radius,y-radius*.4f,x-radius,y+radius,x,y+radius);drop.cubicTo(x+radius,y+radius,x+radius,y-radius*.4f,x,y-radius*2);drop.close();
  paint.setColor(Color.WHITE);paint.setAlpha(Math.round(alpha*220));paint.setStyle(Paint.Style.FILL);c.drawPath(drop,paint);
  paint.setColor(Color.BLACK);paint.setAlpha(Math.round(alpha*220));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(.5f,radius*.22f));c.drawPath(drop,paint);
  paint.setAlpha(255);
 }
}
