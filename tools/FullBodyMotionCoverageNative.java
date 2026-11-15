package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.pm.*;import android.content.res.*;import android.graphics.*;import android.view.*;import java.io.*;import java.util.zip.*;import org.json.*;
/** Offline synthetic inspection of every source pose; no camera, user data or network. */
public final class FullBodyMotionCoverageNative {
 public static int run(Context host)throws Exception{
  File apk=new File(host.getFilesDir(),"glass.apk");AssetManager am=AssetManager.class.getDeclaredConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(am,apk.getPath());ApplicationInfo info=new ApplicationInfo(host.getApplicationInfo());info.sourceDir=info.publicSourceDir=apk.getPath();Context c=new ContextWrapper(host){public AssetManager getAssets(){return am;}public ApplicationInfo getApplicationInfo(){return info;}};
  FullBodyMascotView view=new FullBodyMascotView(c);view.layout(0,0,480,640);Bitmap a=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888);Canvas ca=new Canvas(a),cb=new Canvas(b);int[] before=new int[480*640],after=new int[before.length];JSONArray rows=new JSONArray(),staticIds=new JSONArray();int checks=0;
  try(ZipFile z=new ZipFile(apk)){
   for(int pose=0;pose<FullBodyRealisticAssets.REGIONS.length;pose++){
    Bitmap source=FullBodyRealisticAssets.decode(z,pose,1);ca.drawColor(Color.BLACK);cb.drawColor(Color.BLACK);
    view.renderInspection(ca,source,pose,1100,false,true,null,-1,0);view.renderInspection(cb,source,pose,2700,false,true,null,-1,0);
    a.getPixels(before,0,480,0,0,480,640);b.getPixels(after,0,480,0,0,480,640);int changed=0;
    for(int i=0;i<before.length;i++)if(before[i]!=after[i])changed++;
    rows.put(new JSONObject().put("pose",pose).put("changedPixels",changed));if(changed<30)staticIds.put(pose);source.recycle();checks++;
   }
   View target=new View(c){public boolean post(Runnable task){task.run();return true;}};
   ComfortPoseRenderer comfort=new ComfortPoseRenderer(target);
   try{for(int pose=0;pose<9;pose++){
    comfort.request(pose);long end=android.os.SystemClock.uptimeMillis()+15000;while(!comfort.ready(pose)&&android.os.SystemClock.uptimeMillis()<end)Thread.sleep(10);if(!comfort.ready(pose))throw new AssertionError("rest decode "+pose);
    ca.drawColor(Color.BLACK);cb.drawColor(Color.BLACK);comfort.presence(true);comfort.draw(ca,480,640,pose,0,12000,12600,false,0,0);comfort.draw(cb,480,640,pose,0,14000,15400,false,0,0);
    int changed=changed(a,b,before,after);if(changed<30)throw new AssertionError("rest static after entry "+pose+" "+changed);checks++;
    rows.put(new JSONObject().put("restPose",pose).put("changedPixels",changed));
    if(pose==0||pose==2||pose==4){save(host,a,"rest-"+pose+"-a.png");save(host,b,"rest-"+pose+"-b.png");}
    ca.drawColor(Color.BLACK);cb.drawColor(Color.BLACK);comfort.presence(false);comfort.draw(ca,480,640,pose,0,12000,12600,false,0,0);comfort.draw(cb,480,640,pose,0,14000,15400,false,0,0);
    if(changed(a,b,before,after)!=0)throw new AssertionError("rest body stop ignored "+pose);checks++;
   }}finally{comfort.release();}
   FullBodyGestureRenderer gestures=new FullBodyGestureRenderer(target);
   try{for(int clip=0;clip<44;clip++){
    gestures.request(clip);long end=android.os.SystemClock.uptimeMillis()+15000;while(!gestures.ready(clip)&&android.os.SystemClock.uptimeMillis()<end)Thread.sleep(10);
    while(!gestures.draw(ca,480,640,clip,9000,30000)&&android.os.SystemClock.uptimeMillis()<end)Thread.sleep(10);
    ca.drawColor(Color.BLACK);cb.drawColor(Color.BLACK);if(!gestures.draw(ca,480,640,clip,9000,31100)||!gestures.draw(cb,480,640,clip,9000,33900))throw new AssertionError("gesture unavailable "+clip);
    int changed=changed(a,b,before,after);if(changed<30)throw new AssertionError("gesture static while held on keyframe "+clip+" "+changed);checks++;
    rows.put(new JSONObject().put("gestureClip",clip).put("changedPixels",changed));
   }}finally{gestures.release();}
  }finally{view.releaseInspection();a.recycle();b.recycle();am.close();}
  if(staticIds.length()>0)throw new AssertionError("static ordinary source poses: "+staticIds);
  JSONObject report=new JSONObject().put("sourcePoses",443).put("restPoses",9).put("gestureClips",44).put("frames",rows).put("staticPoseIds",staticIds).put("externalApiCalls",0).put("cameraOpened",false);
  try(FileOutputStream out=host.openFileOutput("motion-coverage.json",0)){out.write(report.toString(2).getBytes("UTF-8"));}return checks;
 }
 static int changed(Bitmap a,Bitmap b,int[] before,int[] after){a.getPixels(before,0,480,0,0,480,640);b.getPixels(after,0,480,0,0,480,640);int changed=0;for(int i=0;i<before.length;i++)if(before[i]!=after[i])changed++;return changed;}
 static void save(Context host,Bitmap b,String name)throws Exception{try(FileOutputStream out=host.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
