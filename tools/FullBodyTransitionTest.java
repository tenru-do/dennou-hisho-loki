package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyTransitionTest {
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
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decodeTransition",int.class);decode.setAccessible(true);
  long bytes=0;
  for(int kind=1;kind<=2;kind++){
   Bitmap[] frames=(Bitmap[])decode.invoke(view,kind);if(frames==null||frames.length!=6)throw new AssertionError("Frames");
   for(int i=0;i<6;i++){
    Bitmap frame=frames[i];bytes+=frame.getAllocationByteCount();
    if(FullBodyTransitionPolicy.frame(i*105,false)!=i||FullBodyTransitionPolicy.frame(i*105,true)!=5-i)throw new AssertionError("Reverse order");
    Bitmap output=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(output);c.drawColor(android.graphics.Color.BLACK);
    float scale=Math.min(240f/frame.getWidth(),616f*FullBodyTransitionPolicy.height(kind,i)/frame.getHeight());float w=frame.getWidth()*scale,h=frame.getHeight()*scale;
    c.drawBitmap(frame,null,new android.graphics.RectF((240-w)/2,616-h,(240+w)/2,616),new android.graphics.Paint(3));
    try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-transition-"+kind+"-"+i+".png")){output.compress(Bitmap.CompressFormat.PNG,100,out);}output.recycle();frame.recycle();
   }
  }
  if(bytes>4000000)throw new AssertionError("Cache memory");
  if(FullBodyTransitionPolicy.kind(165,4)!=1||FullBodyTransitionPolicy.kind(4,165)!=1||FullBodyTransitionPolicy.kind(4,0)!=2||FullBodyTransitionPolicy.kind(-1,4)!=0||!FullBodyTransitionPolicy.reverse(1,4,165))throw new AssertionError("Routing");
  System.out.println("PASS 12 transition frames, reverse playback, posture/gesture routing; cached bytes="+bytes);
 }
}
