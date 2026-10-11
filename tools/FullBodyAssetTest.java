package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyAssetTest {
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
  FullBodyMascotView view=new FullBodyMascotView(context);
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);
  long maximum=0;
  for(int id=0;id<168;id++){
   Bitmap image=(Bitmap)decode.invoke(view,id,480);
   if(image==null||image.getWidth()<=0||image.getHeight()<=0)throw new AssertionError("expression "+id);
   if(image.getHeight()>1024)throw new AssertionError("unbounded crop "+id);
   int[] region=FullBodyCropRegions.REGIONS[id];
   if(image.getWidth()!=region[2]-region[0]||image.getHeight()!=region[3]-region[1])throw new AssertionError("Reviewed region "+id);
   if(id==4) { if(image.getWidth()<100||image.getHeight()<600)throw new AssertionError("004 full hair and no label"); try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-004-corrected.png")){image.compress(Bitmap.CompressFormat.PNG,100,out);} }
   maximum=Math.max(maximum,image.getAllocationByteCount());image.recycle();
  }
  Method idle=FullBodyMascotView.class.getDeclaredMethod("decodeIdle");idle.setAccessible(true);
  Bitmap[] frames=(Bitmap[])idle.invoke(view);
  if(frames==null||frames.length!=6)throw new AssertionError("Idle frame count");
  int w=frames[0].getWidth(),h=frames[0].getHeight();long idleBytes=0;
  for(Bitmap frame:frames){
   if(frame.getWidth()!=w||frame.getHeight()!=h)throw new AssertionError("Aligned idle sizes");
   idleBytes+=frame.getAllocationByteCount();frame.recycle();
  }
  System.out.println("PASS: 168 expression regions decoded; maximum bitmap bytes="+maximum);
  System.out.println("PASS: six aligned idle frames; total bytes="+idleBytes);
  for(int id=169;id<=180;id++){
   Bitmap image=(Bitmap)decode.invoke(view,id,616);if(image==null||image.getHeight()<600)throw new AssertionError("Motion image "+id);
   try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-catalog-"+id+".png")){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
  }
  System.out.println("PASS: all 12 idle/walk images decoded separately");
 }
}

