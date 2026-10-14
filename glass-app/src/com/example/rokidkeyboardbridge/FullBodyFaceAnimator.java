package com.example.rokidkeyboardbridge;
import android.graphics.*;
/** Realistic full-body facial movement; the portrait renderer is independent. */
final class FullBodyFaceAnimator {
 private static final float[] MOUTH_LEVELS={0,.4f,.8f,.3f,0,.6f,1,.4f,0,.3f,.7f,0};
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF rect=new RectF();
 private final Path clip=new Path(),line=new Path();
 private final Matrix skinMatrix=new Matrix();
 private Bitmap skinSource;
 private BitmapShader skinShader;
 void prepare(Bitmap source){skinSource=source;skinShader=new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
 void clear(){skinSource=null;skinShader=null;p.setShader(null);}
 static float[] features(int pose){return pose>=0&&pose<FullBodyRealisticFeatures.ALL.length?FullBodyRealisticFeatures.ALL[pose]:null;}
 static boolean supports(int pose){return features(pose)!=null;}
 static boolean transition(int pose){return (pose>=186&&pose<=221)||(pose>=305&&pose<341);}
 static float blink(long elapsed){long t=Math.max(0,elapsed)%5700;if(t<4500||t>=4740)return 0;if(t<4580)return(t-4500)/80f;if(t<4660)return 1;return(4740-t)/80f;}
 static float mouth(long elapsed,boolean speaking){
  if(!speaking)return 0;
  long time=Math.max(0,elapsed);int index=(int)(time/110%MOUTH_LEVELS.length);
  float t=time%110/110f;t=t*t*(3-2*t);
  return MOUTH_LEVELS[index]+(MOUTH_LEVELS[(index+1)%MOUTH_LEVELS.length]-MOUTH_LEVELS[index])*t;
 }
 void draw(Canvas canvas,Bitmap source,int pose,long elapsed,boolean speaking){
  drawFeatures(canvas,source,features(pose),elapsed,speaking,true,FullBodyClothMotion.base(pose)==1||(pose>=305&&pose<341));
 }
 void drawFeatures(Canvas canvas,Bitmap source,float[] f,long elapsed,boolean speaking,boolean blinking){
  drawFeatures(canvas,source,f,elapsed,speaking,blinking,false);
 }
 private void drawFeatures(Canvas canvas,Bitmap source,float[] f,long elapsed,boolean speaking,boolean blinking,boolean preserveSmile){
  if(f==null||source==null||source.isRecycled())return;
  canvas.save();canvas.scale(source.getWidth()/f[0],source.getHeight()/f[1]);
  float closed=blinking?blink(elapsed):0;
  if(closed>0){if(f[4]>0)eyeMotion(canvas,source,f,2,closed);if(f[10]>0&&Math.abs(f[8]-f[2])>f[4]*.4f)eyeMotion(canvas,source,f,8,closed);}
  float open=speaking?mouth(elapsed,true):preserveSmile?.12f*(.5f+.5f*(float)Math.sin(elapsed/950.0)):0;if(open>0&&f[16]>0)mouth(canvas,source,f,open,preserveSmile);
  canvas.restore();
 }
 // Retain the original lashes, iris, lip corners and shading rather than drawing generic eyes.
 private void patch(Canvas c,Bitmap source,float[] f,float x,float y,float w,float h,float angle,float stretch){
  c.save();c.rotate(angle,x,y);rect.set(x-w,y-h,x+w,y+h);clip.reset();clip.addOval(rect,Path.Direction.CW);c.clipPath(clip);
  c.rotate(-angle,x,y);c.translate(x,y);c.rotate(angle);c.scale(1,stretch);c.rotate(-angle);c.translate(-x,-y);
  c.scale(f[0]/source.getWidth(),f[1]/source.getHeight());p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawBitmap(source,0,0,p);c.restore();
 }
 private void eyeMotion(Canvas c,Bitmap source,float[]f,int offset,float closed){
  if(f[offset+5]>0){eye(c,source,f,offset,closed);return;}
  // Closed lids tighten briefly; never invent an iris over a closed/occluded eye.
  patch(c,source,f,f[offset],f[offset+1],f[offset+2]*.55f,Math.max(1,f[offset+2]*.17f),f[offset+3],1-.14f*closed);
 }
 private void eye(Canvas c,Bitmap source,float[]f,int offset,float closed){
  float x=f[offset],y=f[offset+1],w=f[offset+2],angle=f[offset+3];
  float h=Math.max(w*.21f,f[offset+4]*.65f);
  if(skinSource!=source){skinSource=source;skinShader=new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
  skinMatrix.setScale(f[0]/source.getWidth(),f[1]/source.getHeight());skinMatrix.postTranslate(0,-w*.34f);skinShader.setLocalMatrix(skinMatrix);
  c.save();c.rotate(angle,x,y);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(skinShader);rect.set(x-w*.58f,y-h,x+w*.58f,y+h);c.drawOval(rect,p);p.setShader(null);c.restore();
  if(closed<.85f)patch(c,source,f,x,y,w*.58f,h,angle,Math.max(.05f,1-closed));
  if(closed>.5f){c.save();c.rotate(angle,x,y);p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(.65f,w*.055f));line.reset();line.moveTo(x-w*.49f,y);line.quadTo(x,y+w*.09f*closed,x+w*.49f,y);c.drawPath(line,p);c.restore();}
 }
 private void mouth(Canvas c,Bitmap source,float[]f,float open,boolean preserveSmile){
  float x=f[14],y=f[15],w=f[16],angle=f[17],original=f[18];
  if(preserveSmile){
   // Keep the smiling corners and tilt while opening the centre of the lips.
   patch(c,source,f,x,y,w*.49f,Math.max(original*.8f,w*.15f),angle,.95f+.75f*open);
   if(open>.18f){
    c.save();c.rotate(angle,x,y);float rx=w*.34f,gap=w*.16f*open,curve=w*.035f;
    line.reset();line.moveTo(x-rx,y-curve);line.quadTo(x,y+curve-gap*.25f,x+rx,y-curve);line.quadTo(x,y+curve+gap,x-rx,y-curve);line.close();
    p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.BLACK);c.drawPath(line,p);
    if(open>.45f){p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.5f,w*.035f));line.reset();line.moveTo(x-rx*.63f,y);line.quadTo(x,y+curve-gap*.12f,x+rx*.63f,y);c.drawPath(line,p);p.setStyle(Paint.Style.FILL);}
    c.restore();
   }
  }else if(original>w*.17f){
   // Strong open-mouth emotions stay recognizable, with a small speech deformation.
   float h=Math.max(w*.23f,original*.72f);
   patch(c,source,f,x,y,w*.63f,h,angle,.88f+.27f*open);
  }else{
   c.save();c.rotate(angle,x,y);p.setStyle(Paint.Style.FILL);p.setColor(Color.BLACK);
   float h=Math.max(.4f,w*.18f*open);rect.set(x-w*(.34f-.035f*open),y-h,x+w*(.34f-.035f*open),y+h);c.drawOval(rect,p);
   if(open>.65f){p.setColor(Color.WHITE);rect.set(x-w*.20f,y-h+.25f,x+w*.20f,y-h+Math.max(.55f,w*.035f));c.drawOval(rect,p);}c.restore();
  }
 }
}
