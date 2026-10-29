package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import java.lang.reflect.*;import java.io.*;import java.util.zip.*;
/** Actual Android View controls and Canvas, only in the disposable inspection package. */
public final class ContextVisualDeviceChecks {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static Field field(String s)throws Exception{Field f=FullBodyMascotView.class.getDeclaredField(s);f.setAccessible(true);return f;}
 public static int run(Context system,String apk)throws Exception{
  AvatarPipelineDeviceChecks.normalInspection(system);AssetManager a=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(a,apk);Resources r=new Resources(a,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());Context c=new ContextWrapper(system){public AssetManager getAssets(){return a;}public Resources getResources(){return r;}};
  FullBodyMascotView view=new FullBodyMascotView(c);view.layout(0,0,240,600);EmbodiedScenePolicy state=(EmbodiedScenePolicy)field("embodied").get(view);
  view.setUserContext("腰掛けて");ok(state.posture()==1,"actual alias sets seated posture");view.setUserContext("その姿勢は楽？");ok(state.posture()==1,"question retains pose");
  view.setUserContext("座って、膝を抱えて、こちらを見る");ok(state.correctedRest()&&state.restMotion(100)==8,"compound seated knee pose");
  view.setUserContext("寝そべって");view.setUserContext("そのまま手を振って");ok(state.correctedRest()&&state.restMotion(100)==4,"wave keeps actual lying renderer");
  view.setUserContext("立って");view.setMode(2);view.setUserContext("話し終わったら座って");ok(state.posture()==0&&field("deferredSteps").get(view)!=null,"conditional does not execute early");view.setUserContext("うん");ok(field("deferredSteps").get(view)!=null,"acknowledgement does not cancel conditional");view.setMode(0);ok(state.posture()==1,"speech end executes conditional");
  view.setMode(2);view.setUserContext("話し終わったら立って");view.setUserContext("座って");view.setMode(0);ok(state.posture()==1,"new user command cancels deferred action");
  view.preserveForNotification();view.setExpression(2);view.restoreAfterNotification();ok(state.posture()==1,"notification restores prior posture");
  view.preserveForNotification();view.setUserContext("立って");view.restoreAfterNotification();ok(state.posture()==0,"notification cannot undo newer request");
  view.setUserContext("座って");view.setUserContext("脚を組み替えて");int before=state.selected(android.os.SystemClock.uptimeMillis());view.setSpeechContext("脚を組み替えて");ok(state.selected(android.os.SystemClock.uptimeMillis())==before,"speech context cannot repeat leg change");
  view.setUserContext("座って");view.setUserContext("この座り方が好き");view.setUserContext("立って");view.setUserContext("いつもの姿勢にして");ok(state.posture()==1,"explicit local preference is used");view.clearPreferredPosture();
  view.previewEmbodied(EmbodiedScenePolicy.RELAXED,0);view.setUserContext("立って");view.setUserContext("今の動きを戻して");ok(state.posture()==1,"button and voice share undo history");
  view.setUserContext("座って");view.runVisualScenario();view.setUserContext("もっとゆっくり");ok(field("scenarioSaved").get(view)==null,"real adjustment cancels preview and restores history");
  view.setUserContext("手を振ってから立って");view.setHostResumed(false);ok(field("deferredSteps").get(view)==null,"sleep clears pending work");view.setHostResumed(true);
  RestArtworkDeviceChecks.warm(view,c);
  Bitmap out=Bitmap.createBitmap(240*3,600,Bitmap.Config.ARGB_8888);Canvas sheet=new Canvas(out);sheet.drawColor(Color.BLACK);
  try(ZipFile zip=new ZipFile(apk)){for(int i=0;i<3;i++){view.setUserContext(i==0?"立って":i==1?"座って":"ねそべって");AvatarPipelineDeviceChecks.installFrame(view,zip,i==0?4:168);Bitmap b=Bitmap.createBitmap(240,600,Bitmap.Config.ARGB_8888);view.onDraw(new Canvas(b));field("blendStarted").setLong(view,android.os.SystemClock.uptimeMillis()-10000);b.eraseColor(Color.BLACK);view.onDraw(new Canvas(b));sheet.drawBitmap(b,i*240,0,null);b.recycle();}}
  Bitmap clipped=Bitmap.createBitmap(480,600,Bitmap.Config.ARGB_8888);Canvas cropCanvas=new Canvas(clipped);cropCanvas.drawColor(Color.MAGENTA);view.draw(cropCanvas);boolean untouched=true;for(int y=0;y<600;y++)for(int x=240;x<480;x++)if(clipped.getPixel(x,y)!=Color.MAGENTA)untouched=false;ok(untouched,"real View clips overflow without drawing outside viewport");clipped.recycle();
  for(int pose=0;pose<9;pose++)ok(Math.abs(ComfortPoseGeometry.scale(pose,240,600)*ComfortPoseGeometry.HEAD_WIDTH[pose]-CharacterViewport.standingHead(240,600))<.01,"same head unit independent of pose width");
  try(FileOutputStream f=new FileOutputStream(new File(system.getFilesDir(),"context-v688-native.png"))){out.compress(Bitmap.CompressFormat.PNG,100,f);}out.recycle();view.releaseInspection();CharacterMotionTuning.intentGain=CharacterMotionTuning.intentSpeed=1;AvatarPipelineDeviceChecks.normalInspection(system);return n;
 }
}
