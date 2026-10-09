package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyBlendTest {
 public static void main(String[] args) throws Exception {
  Looper.prepareMainLooper();
  Class<?> at=Class.forName("android.app.ActivityThread");
  Object thread=at.getMethod("systemMain").invoke(null);
  Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();
  if ((int)AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,args[0])==0) throw new AssertionError("APK assets");
  Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());
  Context context=new ContextWrapper(system){
   public android.content.pm.ApplicationInfo getApplicationInfo(){android.content.pm.ApplicationInfo info=new android.content.pm.ApplicationInfo();info.sourceDir=args[0];return info;}
   public AssetManager getAssets(){return assets;}
   public Resources getResources(){return resources;}
  };
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,240,616);
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);
  Method draw=FullBodyMascotView.class.getDeclaredMethod("drawFrame",android.graphics.Canvas.class,Bitmap.class,int.class,long.class,float.class,float.class,boolean.class);draw.setAccessible(true);
  int checks=0;
  for(int id:new int[]{4,168,165,32}){
   Bitmap source=(Bitmap)decode.invoke(view,id,616);float height=FullBodyMotionPolicy.bodyHeightFraction(id);
   Bitmap reference=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(reference);c.drawColor(android.graphics.Color.BLACK);draw.invoke(view,c,source,id,0L,height,1f,false);
   for(float blend:new float[]{0f,.1f,.25f,.5f,.75f,.9f,1f}){
    Bitmap output=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);c=new android.graphics.Canvas(output);c.drawColor(android.graphics.Color.BLACK);
    draw.invoke(view,c,source,id,0L,height,1-blend,false);draw.invoke(view,c,source,id,0L,height,blend,true);
    for(int y=0;y<616;y++)for(int x=0;x<240;x++){int a=reference.getPixel(x,y),b=output.getPixel(x,y);for(int shift:new int[]{0,8,16})if(Math.abs((a>>shift&255)-(b>>shift&255))>3)throw new AssertionError("Blend brightness/face drift "+id+" at "+blend);checks++;}
    output.recycle();
   }
   Bitmap inhale=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);c=new android.graphics.Canvas(inhale);c.drawColor(android.graphics.Color.BLACK);draw.invoke(view,c,source,id,2600L,height,1f,false);
   float scale=FullBodyScalePolicy.scale(id,source.getWidth(),source.getHeight(),240,616);float top=616-source.getHeight()*scale;int moved=0;
   for(int y=0;y<616;y++)for(int x=0;x<240;x++)if(reference.getPixel(x,y)!=inhale.getPixel(x,y)){
    int a=reference.getPixel(x,y),b=inhale.getPixel(x,y);int change=Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>8&255)-(b>>8&255)),Math.abs((a>>16&255)-(b>>16&255))));
    if(change<=3)continue;
    moved++;if(y<top+source.getHeight()*scale*.16f||y>top+source.getHeight()*scale*.52f)throw new AssertionError("Breathing moved head or lower body id="+id+" x="+x+" y="+y+" delta="+change);
   }
   if(moved==0)throw new AssertionError("Breathing invisible in actual Canvas");
   if(id==4){try(java.io.FileOutputStream stream=new java.io.FileOutputStream("/data/local/tmp/loki-breath-exhale.png")){reference.compress(Bitmap.CompressFormat.PNG,100,stream);}try(java.io.FileOutputStream stream=new java.io.FileOutputStream("/data/local/tmp/loki-breath-inhale.png")){inhale.compress(Bitmap.CompressFormat.PNG,100,stream);}}
   inhale.recycle();reference.recycle();source.recycle();
  }
  if(FullBodyBlendPolicy.ease(0)!=0||FullBodyBlendPolicy.ease(1)!=1||FullBodyBlendPolicy.ease(-1)!=0||FullBodyBlendPolicy.ease(2)!=1)throw new AssertionError("Endpoints");
  float prior=0;for(int t=0;t<=630;t++){
   float position=FullBodyBlendPolicy.position(t,630,7);if(position<prior||position-prior>.012f)throw new AssertionError("Timeline jump");prior=position;
  }
  float[] mesh=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
  for(long time=0;time<=10400;time+=50){
   FullBodyBreathPolicy.mesh(mesh,262,735,time);
   int index=0;for(int row=0;row<=FullBodyBreathPolicy.ROWS;row++)for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++){
    float x=262f*col/FullBodyBreathPolicy.COLUMNS,y=735f*row/FullBodyBreathPolicy.ROWS,dx=mesh[index++]-x,dy=mesh[index++]-y;
    if(Math.abs(dx)>1.6f||Math.abs(dy)>1.85f)throw new AssertionError("Breath amplitude");
    if((row/(float)FullBodyBreathPolicy.ROWS<=.18f||row/(float)FullBodyBreathPolicy.ROWS>=.5f)&&(dx!=0||dy!=0))throw new AssertionError("Head/floor breathing drift");
   }
  }
  for(int width:new int[]{180,240,320})for(int height:new int[]{480,598,616}){
   float expected=FullBodyScalePolicy.commonSpan(width,height);
   for(int id=0;id<FullBodyRealisticFeatures.ALL.length;id++)for(int sample:new int[]{1,2}){
    float[] feature=FullBodyRealisticFeatures.ALL[id];int w=(int)Math.ceil(feature[0]/sample),h=(int)Math.ceil(feature[1]/sample);
    float scale=FullBodyScalePolicy.scale(id,w,h,width,height),span=FullBodyScalePolicy.HEAD_SPAN[id]*w/feature[0]*scale;
    if(width>=240 && (id==4||id==135||id==165||id==167||id==168||id>=181&&id<=185) && Math.abs(span-expected)>.8f || w*scale>width+.01f || h*scale>height+.01f)throw new AssertionError("Head scale or bounds "+id+" span="+span+" expected="+expected);
   }
  }
  if(prior!=7)throw new AssertionError("Target endpoint");
  System.out.println("PASS continuous seven-segment transition; source/target endpoints; layered face/body additive blending preserves brightness: "+checks+" pixel checks; standing reference scale; seated head scale matches; all full-body frames remain in bounds");
 }
}
