package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyIdleActivityTest {
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
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,240,598);set(view,"breathingEnabled",false);
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);
  Method group=FullBodyMascotView.class.getDeclaredMethod("decodeGroup",int[].class,int.class);group.setAccessible(true);
  Method draw=FullBodyMascotView.class.getDeclaredMethod("onDraw",android.graphics.Canvas.class);draw.setAccessible(true);
  Bitmap baseImage=(Bitmap)decode.invoke(view,168,598);set(view,"bitmap",baseImage);set(view,"loaded",168);
  java.io.File output=new java.io.File("/data/local/tmp/loki-links-v427");output.mkdirs();long maxBytes=0;
  for(int kind=0;kind<2;kind++){
   Bitmap[] frames=(Bitmap[])group.invoke(view,FullBodyIdleActivity.frames(kind),1);if(frames==null||frames.length!=6)throw new AssertionError("Missing activity");
   long bytes=0;for(Bitmap f:frames)bytes+=f.getAllocationByteCount();if(bytes>8000000)throw new AssertionError("Activity cache");maxBytes=Math.max(maxBytes,bytes);
   set(view,"activityFrames",frames);set(view,"activityCached",kind);
   long start=kind==0?15000:105000;
   java.util.HashSet<Integer> visited=new java.util.HashSet<>();for(long time=start;time<start+(kind==0?30000:12000);time+=50)visited.add(FullBodyIdleActivity.frame(kind,time));if(visited.size()!=6)throw new AssertionError("Unreachable activity frame");
   for(int frame=0;frame<6;frame++){
    Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);Method render=FullBodyMascotView.class.getDeclaredMethod("drawFrame",android.graphics.Canvas.class,Bitmap.class,int.class,long.class,float.class,float.class,boolean.class);render.setAccessible(true);
    render.invoke(view,new android.graphics.Canvas(image),frames[frame],FullBodyIdleActivity.firstFrame(kind)+frame,0L,.76f,1f,false);
    try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(output,"activity-"+kind+"-"+frame+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}image.recycle();
   }
   for(int mode=0;mode<3;mode++){
    long now=android.os.SystemClock.uptimeMillis();set(view,"mode",mode);set(view,"idleStarted",now-start-2000);set(view,"motionStarted",now);set(view,"previousBitmap",null);set(view,"lastDrawnBitmap",null);
    Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(image));
    java.lang.reflect.Field last=view.getClass().getDeclaredField("lastDrawnPose");last.setAccessible(true);int displayed=last.getInt(view);
    if((mode==0)!=FullBodyIdleActivity.isFrame(displayed))throw new AssertionError("Speech/listening did not preempt activity");image.recycle();
   }
   for(Bitmap f:frames)f.recycle();
  }
  if(FullBodyIdleActivity.kind(14999)!=-1||FullBodyIdleActivity.kind(15000)!=0||FullBodyIdleActivity.kind(45000)!=-1||FullBodyIdleActivity.kind(105000)!=1||FullBodyIdleActivity.kind(117000)!=-1)throw new AssertionError("Activity cadence");
  if(FullBodyFaceAnimator.features(230)[16]!=0||FullBodyFaceAnimator.features(231)[16]!=0)throw new AssertionError("Covered coffee mouth would animate");
  System.out.println("PASS tablet/coffee six-frame sequences, cadence, actual view preempted by speech/listening, covered mouths suppressed; max one-activity bytes="+maxBytes);
 }
 static void set(Object target,String name,Object value)throws Exception{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
}
