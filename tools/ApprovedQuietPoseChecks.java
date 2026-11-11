package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import android.os.*;import android.view.View;import java.io.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class ApprovedQuietPoseChecks {
 static int count;static final Handler main=new Handler(Looper.getMainLooper());
 static void check(boolean ok){count++;if(!ok)throw new AssertionError("check "+count);}
 static void ui(Runnable task)throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();main.post(()->{try{task.run();}catch(Throwable e){error.set(e);}finally{done.countDown();}});if(!done.await(10,TimeUnit.SECONDS))throw new Exception("UI timeout");if(error.get()!=null)throw new RuntimeException(error.get());}
 public static int run(Context c)throws Exception{
  count=0;check(ApprovedQuietPosePolicy.sofa("ロキ、ソファでくつろいで"));check(!ApprovedQuietPosePolicy.sofa("ソファでくつろぐ方法を教えて"));check(!ApprovedQuietPosePolicy.sofa("ソファに座らないで"));check(QuietPosePerformance.samePosture(7,0)&&!QuietPosePerformance.samePosture(7,4));check(ApprovedQuietPosePolicy.seated(1,false,true,999999)==0);check(ApprovedQuietPosePolicy.seated(0,true,true,999999)==0);check(ApprovedQuietPosePolicy.seated(0,false,false,999999)==0);
  check(ApprovedQuietPosePolicy.seated(0,false,true,45000)==0);check(ApprovedQuietPosePolicy.seated(0,false,true,90000)==0);check(ApprovedQuietPosePolicy.resting(false,999999)==3);check(ApprovedQuietPosePolicy.resting(true,60000)==4);check(ApprovedQuietPosePolicy.resting(true,120000)==3);
  for(int[] size:new int[][]{{480,640},{320,900},{900,320}})for(int[] image:new int[][]{{832,1248},{340,754},{275,590}}){float[] fit=ApprovedQuietPosePolicy.fit(size[0],size[1],image[0],image[1]);check(fit[1]>=size[1]*.019f&&fit[2]<=size[0]*1.057f&&fit[3]<=size[1]*1.057f);check(Math.abs(fit[2]/fit[3]-image[0]/(float)image[1])<.0001);check(Math.abs(fit[2]/image[0]-Math.min(size[0]*.96f/image[0],size[1]*.96f/image[1])*1.10f)<.001f);check(Math.abs(fit[2]/image[0]-Math.min(size[0]*.96f/image[0],size[1]*.96f/image[1])*1.10f)<.001f);}
  check(ApprovedQuietPosePolicy.fade(-1)==0&&ApprovedQuietPosePolicy.fade(1000)==1);
  QuietPosePerformance feedback=new QuietPosePerformance();feedback.update(0,CharacterChoreography.IDLE,0,FullBodyPoseCatalog.Kind.NEUTRAL,0);check(feedback.emotion()==QuietPosePerformance.CALM);
  feedback.accepted(1000,false);check(feedback.acceptance(1100)>0);float once=feedback.acceptance(1150);feedback.accepted(1100,false);check(feedback.acceptance(1150)==once);check(!feedback.mayChange(2000,false));check(feedback.mayChange(2000,true));
  feedback.update(1,CharacterChoreography.THINK,0,FullBodyPoseCatalog.Kind.THINK,2200);check(feedback.emotion()==QuietPosePerformance.THOUGHT&&feedback.gazeY()>0);check(!feedback.mayChange(20000,false));check(feedback.ready(2300)==0);
  feedback.update(2,CharacterChoreography.SPEAK,0,FullBodyPoseCatalog.Kind.JOY,2400);check(feedback.emotion()==QuietPosePerformance.JOY&&feedback.ready(2600)>0);
  feedback.update(0,CharacterChoreography.IDLE,0,FullBodyPoseCatalog.Kind.WORRY,3000);check(feedback.emotion()==QuietPosePerformance.CONCERN);check(!feedback.mayChange(8000,false)&&feedback.mayChange(11000,false));
  feedback.update(0,CharacterChoreography.IDLE,2,FullBodyPoseCatalog.Kind.RECOVERY,12000);check(feedback.emotion()==QuietPosePerformance.RELIEF&&feedback.gain()<.5f);check(QuietPosePerformance.cycleAge(90000,2)==45000);
  feedback.update(0,CharacterChoreography.NAV,1,FullBodyPoseCatalog.Kind.NEUTRAL,13000);feedback.accepted(13100,false);check(feedback.gain()==0&&feedback.nod(13200)==0);
  check(QuietPosePerformance.transitionStage(100)==0&&QuietPosePerformance.transitionStage(500)==1&&QuietPosePerformance.transitionStage(1000)==2&&QuietPosePerformance.transitionStage(1300)==3);
  check(QuietPosePerformance.transitionBlend(239)==0&&QuietPosePerformance.transitionBlend(800)==1);check(QuietPosePerformance.prepare(100)>0&&QuietPosePerformance.settle(1000)>0);
  check(QuietPosePerformance.samePosture(0,2)&&QuietPosePerformance.samePosture(3,5)&&!QuietPosePerformance.samePosture(1,4)&&!QuietPosePerformance.samePosture(-1,2));
  AssetManager assets=AssetManager.class.getDeclaredConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,new File(c.getFilesDir(),"phone.apk").getPath());
  Context wrapper=new ContextWrapper(c){public AssetManager getAssets(){return assets;}};
  AtomicReference<ApprovedQuietPoseRenderer> holder=new AtomicReference<>();ui(()->holder.set(new ApprovedQuietPoseRenderer(new View(wrapper){public boolean post(Runnable task){return main.post(task);}})));ApprovedQuietPoseRenderer renderer=holder.get();
  java.lang.reflect.Field selected=ApprovedQuietPoseRenderer.class.getDeclaredField("selected");selected.setAccessible(true);
  for(int index=0;index<ApprovedQuietPosePolicy.ASSETS.length;index++){
   final int id=index;Bitmap bitmap=Bitmap.createBitmap(512,768,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);ui(()->renderer.draw(canvas,512,768,id,SystemClock.uptimeMillis(),false,false));
   long until=SystemClock.uptimeMillis()+8000;while(selected.getInt(renderer)!=id&&SystemClock.uptimeMillis()<until)Thread.sleep(30);check(selected.getInt(renderer)==id);
   if(id==1){java.lang.reflect.Field started=ApprovedQuietPoseRenderer.class.getDeclaredField("changedAt");started.setAccessible(true);long origin=started.getLong(renderer);for(int stage:new int[]{120,500,1000,1350}){ui(()->renderer.draw(canvas,512,768,id,origin+stage,true,false));try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"quiet-stage-"+stage+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}check(true);}
   final long frameTime=(SystemClock.uptimeMillis()/5700+2)*5700+4500;
   ui(()->renderer.draw(canvas,512,768,id,frameTime,false,false));int[] before=new int[512*768];bitmap.getPixels(before,0,512,0,0,512,768);
   ui(()->renderer.draw(canvas,512,768,id,frameTime+120,true,id<3));int[] after=new int[before.length];bitmap.getPixels(after,0,512,0,0,512,768);int diff=0;for(int i=0;i<before.length;i++)if(before[i]!=after[i])diff++;check(diff>0);
   try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"quiet-pose-"+id+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
  }
  ui(renderer::release);check(selected.getInt(renderer)==-1);
  AtomicReference<FullBodyMascotView> wholeRef=new AtomicReference<>();ui(()->{FullBodyMascotView v=new FullBodyMascotView(wrapper);((android.app.Activity)c).addContentView(v,new android.view.ViewGroup.LayoutParams(512,768));v.layout(0,0,512,768);wholeRef.set(v);});FullBodyMascotView whole=wholeRef.get();
  java.lang.reflect.Field layer=FullBodyMascotView.class.getDeclaredField("approvedQuietArtwork");layer.setAccessible(true);
  java.lang.reflect.Field embodiedField=FullBodyMascotView.class.getDeclaredField("embodied");embodiedField.setAccessible(true);
  ui(()->{try{
   whole.setUserContext("座って");whole.setUserContext("脚を組み替えて");
   ApprovedQuietPoseRenderer integrated=(ApprovedQuietPoseRenderer)layer.get(whole);
   check(integrated!=null&&integrated.legMotionWaiting());
   check(((EmbodiedScenePolicy)embodiedField.get(whole)).clip(SystemClock.uptimeMillis(),false,true)!=31);
   whole.stopVisualMotion();check(!integrated.legMotionWaiting()&&!integrated.legMotionActive());
   whole.setUserContext("脚を組み替えて");check(integrated.legMotionWaiting());
   whole.previewQuietArtwork(1);check(!integrated.legMotionWaiting()&&!integrated.legMotionActive());
   java.lang.reflect.Field choice=FullBodyMascotView.class.getDeclaredField("quietArtworkChoice");choice.setAccessible(true);
   java.lang.reflect.Field held=FullBodyMascotView.class.getDeclaredField("visualHeld");held.setAccessible(true);
   check(choice.getInt(whole)==1);
   whole.setUserContext("ポーズを固定して");
   whole.setUserContext("自然にいろんなポーズをとって");
   check(choice.getInt(whole)==-1&&!held.getBoolean(whole));
   java.lang.reflect.Method quietGetter=FullBodyMascotView.class.getDeclaredMethod("approvedQuietArtwork");quietGetter.setAccessible(true);
   whole.previewQuietArtwork(2);quietGetter.invoke(whole);check(choice.getInt(whole)==2);
   CharacterMotionTuning.naturalPoses=false;quietGetter.invoke(whole);
   CharacterMotionTuning.naturalPoses=true;quietGetter.invoke(whole);
   check(choice.getInt(whole)==-1&&!held.getBoolean(whole));
   whole.setUserContext("脚を組み替えて");check(integrated.legMotionWaiting());
   whole.setUserContext("立って");check(!integrated.legMotionWaiting()&&!integrated.legMotionActive());
   whole.setUserContext("座って");whole.setUserContext("脚を組み替えて");check(integrated.legMotionWaiting());
   whole.setUserContext("寝そべって");check(!integrated.legMotionWaiting()&&!integrated.legMotionActive());
  }catch(ReflectiveOperationException error){throw new RuntimeException(error);}});
  for(int index:new int[]{0,1,2,4,5,7}){
   final int id=index;Bitmap picture=Bitmap.createBitmap(512,768,Bitmap.Config.ARGB_8888);Canvas target=new Canvas(picture);ui(()->whole.previewQuietArtwork(id));
   long until=SystemClock.uptimeMillis()+10000;boolean ready=false;
   while(SystemClock.uptimeMillis()<until){ui(()->whole.draw(target));Object instance=layer.get(whole);if(instance!=null&&selected.getInt(instance)==id){ready=true;break;}Thread.sleep(60);}
   check(ready);Thread.sleep(700);ui(()->whole.draw(target));
   try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"quiet-integrated-"+id+".png"))){picture.compress(Bitmap.CompressFormat.PNG,100,out);}picture.recycle();
  }
  ui(()->whole.setHostResumed(false));check(layer.get(whole)==null);
  AtomicReference<PortraitExpressionRenderer> portraitRef=new AtomicReference<>();ui(()->portraitRef.set(new PortraitExpressionRenderer(wrapper)));PortraitExpressionRenderer portrait=portraitRef.get();check(portrait.ready());Bitmap faceImage=Bitmap.createBitmap(512,768,Bitmap.Config.ARGB_8888);Canvas faceCanvas=new Canvas(faceImage);AtomicBoolean rendered=new AtomicBoolean();
  ui(()->{CharacterChoreography.scene=0;CharacterChoreography.phase=CharacterChoreography.IDLE;rendered.set(portrait.draw(faceCanvas,512,768,0,0,false,false,100000));portrait.react(100100,false,0);});check(rendered.get());
  ui(()->{CharacterChoreography.phase=CharacterChoreography.THINK;rendered.set(portrait.draw(faceCanvas,512,768,0,1,false,false,100200));});check(rendered.get());
  ui(()->{CharacterChoreography.phase=CharacterChoreography.SPEAK;rendered.set(portrait.draw(faceCanvas,512,768,0,2,true,false,100400));});check(rendered.get());
  ui(()->{CharacterChoreography.scene=1;rendered.set(portrait.draw(faceCanvas,512,768,0,0,false,true,100500));});check(rendered.get());
  ui(()->{CharacterChoreography.scene=0;portrait.release();});check(!portrait.ready());faceImage.recycle();int additional=VisualReliabilityChecks.run(c,wrapper);int warm=WarmPresenceChecks.run(c,wrapper);int color=ColorArtworkChecks.run(c,wrapper);assets.close();return count+additional+warm+color;
 }
}
