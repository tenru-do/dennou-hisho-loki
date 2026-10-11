package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodySeatedTest {
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
  FullBodyMascotView view=new FullBodyMascotView(context);Method decode=FullBodyMascotView.class.getDeclaredMethod("decodeSeatedFrames");decode.setAccessible(true);Bitmap[] frames=(Bitmap[])decode.invoke(view);if(frames==null||frames.length!=6)throw new AssertionError("Seated frames");long bytes=0;
  FullBodyFaceAnimator face=new FullBodyFaceAnimator();
  for(int i=0;i<6;i++){
   Bitmap frame=frames[i];bytes+=frame.getAllocationByteCount();Bitmap out=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(out);c.drawColor(android.graphics.Color.BLACK);float scale=616*.76f/frame.getHeight();if(frame.getWidth()*scale>240)throw new AssertionError("Seated body shrinks");float w=frame.getWidth()*scale,h=frame.getHeight()*scale;c.save();c.translate((240-w)/2,616-h);c.scale(scale,scale);c.drawBitmap(frame,0,0,new android.graphics.Paint(3));face.draw(c,frame,FullBodyRealisticAssets.SEATED[i],0,false);c.restore();
   try(java.io.FileOutputStream f=new java.io.FileOutputStream("/data/local/tmp/loki-seated-"+i+".png")){out.compress(Bitmap.CompressFormat.PNG,100,f);}out.recycle();
   Bitmap blink=frame.copy(Bitmap.Config.ARGB_8888,true);face.draw(new android.graphics.Canvas(blink),frame,FullBodyRealisticAssets.SEATED[i],4600,false);int changed=0;for(int y=0;y<frame.getHeight();y++)for(int x=0;x<frame.getWidth();x++)if(frame.getPixel(x,y)!=blink.getPixel(x,y)){changed++;if(y>100)throw new AssertionError("Face offset");}if(changed==0)throw new AssertionError("No blink");blink.recycle();frame.recycle();
  }
  if(FullBodyMotionPolicy.poseForState(0,0)!=168||!FullBodyMotionPolicy.isSeated(168)||FullBodyTransitionPolicy.kind(168,4)!=1)throw new AssertionError("Front seat routing");
  long[]times={0,60000,60150,90000,90150,135000,135150,165000,165150,180000};int[]wanted={0,1,2,1,3,4,5,4,3,0};for(int i=0;i<times.length;i++)if(FullBodySeatedPolicy.frame(times[i])!=wanted[i])throw new AssertionError("Leg cycle "+times[i]);
  if(FullBodySeatedPolicy.waitingPose(179999)!=168||FullBodySeatedPolicy.waitingPose(180000)!=167||FullBodySeatedPolicy.waitingPose(240000)!=165||FullBodySeatedPolicy.waitingPose(300000)!=168)throw new AssertionError("Quiet waiting variation");
  if(bytes>1200000)throw new AssertionError("Seated cache");System.out.println("PASS front seated six frames, fixed height, blink alignment, alternating legs, posture routing; bytes="+bytes);
 }
}
