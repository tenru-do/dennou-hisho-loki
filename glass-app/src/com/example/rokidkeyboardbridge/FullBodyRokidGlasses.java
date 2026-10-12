package com.example.rokidkeyboardbridge;
import android.graphics.*;
/** Clear lenses, broad rounded trapezoid rims and outer camera of the display-equipped Rokid Glasses.
 * Reference: https://jp.rokid.com/products/rokid-glasses . Single-colour contour adaptation. */
final class FullBodyRokidGlasses {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path rim=new Path();
 private final FullBodyAccessoryTimeline timeline=new FullBodyAccessoryTimeline();
 static final long MOTION_MS=FullBodyAccessoryTimeline.DURATION;
 private static final float[] HAND_X={.105f,.066f,.033f,0},HAND_Y={.226f,.12f,.045f,0};
 private final long started=android.os.SystemClock.uptimeMillis();
 boolean wearing(){return timeline.wearing;}
 boolean wantsMotion(long now){return timeline.wants(now-started);}
 boolean moving(){return timeline.moving();}
 boolean attaching(){return timeline.target;}
 long elapsed(long now){return timeline.elapsed(now-started);}
 int pose(int node,int base){return timeline.pose(node,base);}
 void update(long now,int mode,boolean transitioning,boolean ready){timeline.update(now-started,mode,transitioning,ready);} void draw(Canvas canvas,Bitmap bitmap,int pose){
  float[] f=FullBodyFaceAnimator.features(pose);boolean gesture=moving()&&pose>=329&&pose<=334;
  boolean visible=moving()&&elapsed(android.os.SystemClock.uptimeMillis())>MOTION_MS/2?timeline.target:timeline.wearing;
  if((!visible&&!gesture)||f==null)return;
  float distance=(float)Math.hypot(f[8]-f[2],f[9]-f[3]);
  if(distance<8||f[4]<3||f[10]<3)return; // Invisible/occluded face: no floating accessory.
  int save=canvas.save();canvas.scale(bitmap.getWidth()/f[0],bitmap.getHeight()/f[1]);
  float x=(f[2]+f[8])*.5f,y=(f[3]+f[9])*.5f;
  if(gesture&&pose<=332){
   int step=pose-329;
   // Pinched glasses follow the new hand frames from waist to temple. Head coordinates remain unchanged.

   x+=f[1]*HAND_X[step];y+=f[1]*HAND_Y[step];
  }
  canvas.translate(x,y);canvas.rotate((float)Math.toDegrees(Math.atan2(f[9]-f[3],f[8]-f[2])));canvas.scale(distance,distance);
  lens(canvas,-.5f,Math.min(.47f,f[4]/distance),false);
  lens(canvas,.5f,Math.min(.47f,f[10]/distance),true);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.065f);paint.setColor(Color.BLACK);
  rim.reset();rim.moveTo(-.12f,-.07f);rim.quadTo(0,-.17f,.12f,-.07f);canvas.drawPath(rim,paint);
  paint.setStrokeWidth(.012f);paint.setColor(Color.WHITE);canvas.drawPath(rim,paint);
  // Thick electronics at the outer hinges, with the small camera on the front corner.
  paint.setStrokeWidth(.075f);paint.setColor(Color.BLACK);canvas.drawLine(-.96f,-.10f,-1.07f,-.15f,paint);canvas.drawLine(.91f,-.09f,1.04f,-.16f,paint);
  paint.setStrokeWidth(.012f);paint.setColor(Color.WHITE);canvas.drawLine(-.96f,-.10f,-1.07f,-.15f,paint);canvas.drawLine(.91f,-.09f,1.04f,-.16f,paint);
  paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);canvas.drawCircle(.96f,-.13f,.052f,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.012f);paint.setColor(Color.WHITE);canvas.drawCircle(.96f,-.13f,.038f,paint);
  canvas.restoreToCount(save);
 }
 private void lens(Canvas canvas,float centre,float width,boolean far){
  float w=Math.max(.30f,width),h=far?.30f:.32f;
  rim.reset();rim.moveTo(centre-w,-h*.80f);rim.cubicTo(centre-w,-h,centre+w*.75f,-h,centre+w,-h*.70f);
  rim.cubicTo(centre+w*1.05f,0,centre+w*.72f,h*.82f,centre+w*.42f,h);
  rim.cubicTo(centre-w*.20f,h*1.08f,centre-w*.79f,h*.77f,centre-w,h*.22f);rim.close();
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(Color.BLACK);paint.setStrokeWidth(.075f);canvas.drawPath(rim,paint);
  paint.setColor(Color.WHITE);paint.setStrokeWidth(.012f);canvas.drawPath(rim,paint);
 }
}
