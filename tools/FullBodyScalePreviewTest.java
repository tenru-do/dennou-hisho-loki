package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyScalePreviewTest {
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
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,240,598);
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);
  Method draw=FullBodyMascotView.class.getDeclaredMethod("drawFrame",android.graphics.Canvas.class,Bitmap.class,int.class,long.class,float.class,float.class,boolean.class);draw.setAccessible(true);
  for(int id:new int[]{4,168,165}){
   Bitmap source=(Bitmap)decode.invoke(view,id,598);Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.drawColor(android.graphics.Color.BLACK);
   draw.invoke(view,canvas,source,id,0L,FullBodyMotionPolicy.bodyHeightFraction(id),1f,false);
   try(java.io.FileOutputStream stream=new java.io.FileOutputStream("/data/local/tmp/loki-scale-"+id+".png")){image.compress(Bitmap.CompressFormat.PNG,100,stream);}
   source.recycle();image.recycle();
  }
  System.out.println("PASS actual Canvas standing/seated previews, common head span="+FullBodyScalePolicy.commonSpan(240,598));
 }
}
