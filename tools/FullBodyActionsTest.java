package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import android.view.*;import android.os.Looper;import java.lang.reflect.*;
public final class FullBodyActionsTest {
 static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
 public static void main(String[] args)throws Exception{
  FullBodyIdleActivity.automaticPenEnabled=true;
  Looper.prepareMainLooper();Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,args[0]);Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());
  Context context=new ContextWrapper(system){public android.content.pm.ApplicationInfo getApplicationInfo(){android.content.pm.ApplicationInfo i=new android.content.pm.ApplicationInfo();i.sourceDir=args[0];return i;}public AssetManager getAssets(){return assets;}public Resources getResources(){return resources;}};
  boolean[] visual={true};int[] changes={0};FullBodyStatusView status=new FullBodyStatusView(context,()->visual[0],()->changes[0]++);
  for(int i=0;i<20;i++){status.setText("VOICE音声取得 "+i);status.setVisibility(View.VISIBLE);if(status.getVisibility()!=View.GONE)throw new AssertionError("Legacy VOICE status overlaps comments");}
  if(changes[0]<20)throw new AssertionError("Diagnostics not refreshed");visual[0]=false;status.setVisibility(View.VISIBLE);if(status.getVisibility()!=View.VISIBLE)throw new AssertionError("Face status lost");
  for(String text:new String[]{"ジークンドーの動きを見せて","ジートクンドーをやってみて","截拳道を披露して","show Jeet Kune Do"})if(!FullBodyPerformancePolicy.requests(text))throw new AssertionError("Explicit request missed");
  for(String text:new String[]{"ジークンドーとは何？","ジークンドーを見せないで","ジークンドーはやらなくていい","『ジークンドーを見せて』の意味は？","don't show JKD","コーヒーを見せて"})if(FullBodyPerformancePolicy.requests(text))throw new AssertionError("Unexpected action");
  if(FullBodyIdleActivity.kind(464999)==2||FullBodyIdleActivity.kind(465000)!=2||FullBodyIdleActivity.kind(471699)!=2||FullBodyIdleActivity.kind(471700)==2)throw new AssertionError("Rare pen cadence");
  int penWindows=0;boolean active=false;for(long t=0;t<3600000;t+=100){boolean pen=FullBodyIdleActivity.kind(t)==2;if(pen&&!active)penWindows++;active=pen;}if(penWindows!=6)throw new AssertionError("Pen too frequent");
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,240,598);set(view,"breathingEnabled",false);
  Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);Method group=FullBodyMascotView.class.getDeclaredMethod("decodeGroup",int[].class,int.class);group.setAccessible(true);Method draw=FullBodyMascotView.class.getDeclaredMethod("onDraw",Canvas.class);draw.setAccessible(true);
  java.io.File out=new java.io.File("/data/local/tmp/loki-actions-v429");out.mkdirs();long max=0;
  for(int action=0;action<2;action++){
   int base=action==0?168:4;Bitmap source=(Bitmap)decode.invoke(view,base,598);set(view,"bitmap",source);set(view,"loaded",base);set(view,"mode",0);set(view,"previewPose",-1);set(view,"lastDrawnBitmap",null);set(view,"previousBitmap",null);
   Bitmap[] frames=(Bitmap[])group.invoke(view,action==0?FullBodyIdleActivity.frames(2):FullBodyPerformancePolicy.frames(),1);long bytes=0;for(Bitmap b:frames)bytes+=b.getAllocationByteCount();max=Math.max(max,bytes);
   if(action==0){set(view,"activityFrames",frames);set(view,"activityCached",2);}else{set(view,"performanceFrames",frames);set(view,"activityFrames",null);}
   long elapsed=0;
   for(int frame=0;frame<frames.length;frame++){
    long now=android.os.SystemClock.uptimeMillis();if(action==0)set(view,"idleStarted",now-465000-elapsed);else set(view,"performanceStarted",now-elapsed-1);
    set(view,"lastDrawnBitmap",null);set(view,"previousBitmap",null);Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.BLACK);draw.invoke(view,canvas);
    int expected=(action==0?234:FullBodyPerformancePolicy.FIRST)+frame;Field f=FullBodyMascotView.class.getDeclaredField("lastDrawnPose");f.setAccessible(true);if(f.getInt(view)!=expected)throw new AssertionError("Action frame not drawn "+expected+" actual "+f.getInt(view));
    try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(out,(action==0?"pen":"jkd")+"-"+frame+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}image.recycle();elapsed+=(action==0?FullBodyIdleActivity.PEN_MS:FullBodyPerformancePolicy.DURATION)[frame];
   }
   if(action==0){set(view,"mode",1);set(view,"lastDrawnBitmap",null);set(view,"previousBitmap",null);Bitmap image=Bitmap.createBitmap(240,598,Bitmap.Config.ARGB_8888);draw.invoke(view,new Canvas(image));Field f=FullBodyMascotView.class.getDeclaredField("lastDrawnPose");f.setAccessible(true);if(f.getInt(view)>=234)throw new AssertionError("Pen not preempted by listening");image.recycle();}
   for(Bitmap b:frames)b.recycle();source.recycle();
  }
  if(max>8000000)throw new AssertionError("Action cache too big");System.out.println("PASS VOICE overlap prevention, face status restoration, explicit/negative/quoted action routing, rare pen 6/hour, native 18 action exports, listening preemption; max action bytes="+max);
 }
}
