package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodyPoseLinksTest {
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
  java.io.File output=new java.io.File("/data/local/tmp/loki-links-v427");output.mkdirs();long maxBytes=0;
  for(int link=0;link<FullBodyPoseLinks.PAIRS.length;link++){
   int from=FullBodyPoseLinks.PAIRS[link][0],to=FullBodyPoseLinks.PAIRS[link][1];
   if(FullBodyPoseLinks.index(from,to)!=link||FullBodyPoseLinks.index(to,from)!=link||FullBodyPoseLinks.reverse(link,from)||!FullBodyPoseLinks.reverse(link,to))throw new AssertionError("Bidirectional route");
   if(FullBodyMotionPolicy.isSeated(from)!=FullBodyMotionPolicy.isSeated(to))throw new AssertionError("Posture changed by gesture link");
   Bitmap source=(Bitmap)decode.invoke(view,from,598),target=(Bitmap)decode.invoke(view,to,598);
   Bitmap[] frames=(Bitmap[])group.invoke(view,FullBodyPoseLinks.frames(link),1);if(frames==null||frames.length!=FullBodyPoseLinks.frames(link).length)throw new AssertionError("Missing frames");
   long bytes=0;for(int i=0;i<frames.length;i++){
    Bitmap f=frames[i];bytes+=f.getAllocationByteCount();int id=FullBodyPoseLinks.frames(link)[i];
    float scale=FullBodyScalePolicy.scale(id,f.getWidth(),f.getHeight(),240,598);
    float head=FullBodyScalePolicy.HEAD_SPAN[id]*f.getWidth()/FullBodyRealisticFeatures.ALL[id][0]*scale;
    if(id<305&&Math.abs(head-FullBodyScalePolicy.commonSpan(240,598))>.06f)throw new AssertionError("Link head scale "+id);
   }
   if(bytes>8000000)throw new AssertionError("Link cache memory");maxBytes=Math.max(maxBytes,bytes);
   set(view,"bitmap",target);set(view,"loaded",to);set(view,"mode",0);set(view,"transitionFirst",FullBodyPoseLinks.firstFrame(link));
   for(boolean reverse:new boolean[]{false,true}){
    for(int frame=0;frame<=21;frame++){
     long time=frame*30L,now=android.os.SystemClock.uptimeMillis();
     set(view,"previousBitmap",reverse?target:source);set(view,"previousPose",reverse?to:from);set(view,"bitmap",reverse?source:target);set(view,"loaded",reverse?from:to);
     set(view,"transitionFrames",frames);set(view,"transitionIds",FullBodyPoseLinks.frames(link));set(view,"transitionReverse",reverse);set(view,"transitionStarted",now-time);set(view,"motionStarted",now-100L);set(view,"lastDrawnBitmap",null);
     Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(image));
     try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(output,"link-"+link+"-"+(reverse?"reverse":"forward")+"-"+String.format(java.util.Locale.US,"%02d",frame)+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}image.recycle();
    }
   }
   for(Bitmap f:frames)f.recycle();source.recycle();target.recycle();
  }
  if(FullBodyPoseLinks.index(168,4)>=0||FullBodyPoseLinks.index(4,168)>=0||FullBodyPoseLinks.index(-1,4)>=0)throw new AssertionError("Gesture route stole posture or startup");
  if(FullBodyPoseLinks.index(169,6)!=1||FullBodyPoseLinks.index(165,168)!=2||FullBodyPoseLinks.index(168,135)!=3)throw new AssertionError("Approved aliases");
  System.out.println("PASS 6 pose-specific links, 12 directions, actual Canvas endpoint/animation export, common head scale; max one-link bytes="+maxBytes);
 }
 static void set(Object target,String name,Object value)throws Exception{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
}
