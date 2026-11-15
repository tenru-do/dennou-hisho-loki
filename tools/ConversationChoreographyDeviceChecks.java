package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.pm.*;import android.content.res.*;import android.graphics.*;import java.io.*;import java.lang.reflect.*;
public final class ConversationChoreographyDeviceChecks{
 static int n;static void check(boolean value,String label){n++;if(!value)throw new AssertionError(label);}
 public static int run(Context host)throws Exception{
  File apk=new File(host.getFilesDir(),"glass.apk");AssetManager assets=AssetManager.class.getDeclaredConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,apk.getPath());ApplicationInfo app=new ApplicationInfo(host.getApplicationInfo());app.sourceDir=app.publicSourceDir=apk.getPath();Context c=new ContextWrapper(host){public AssetManager getAssets(){return assets;}public ApplicationInfo getApplicationInfo(){return app;}};
  CharacterPreviewGallery gallery=new CharacterPreviewGallery(c){public boolean post(Runnable task){task.run();return true;}};
  long end=android.os.SystemClock.uptimeMillis()+20000;while(!gallery.ready()&&android.os.SystemClock.uptimeMillis()<end)Thread.sleep(20);check(gallery.ready(),"gallery decodes actual production assets");
  for(int[] dims:new int[][]{{480,720},{320,480}}){gallery.layout(0,0,dims[0],dims[1]);Bitmap picture=Bitmap.createBitmap(dims[0],dims[1],Bitmap.Config.ARGB_8888);gallery.draw(new Canvas(picture));int cw=dims[0]/2,ch=dims[1]/3;
   for(int i=0;i<6;i++){int visible=0;for(int y=i/2*ch+30;y<(i/2+1)*ch;y+=3)for(int x=i%2*cw;x<(i%2+1)*cw;x+=3){int p=picture.getPixel(x,y);if(Color.red(p)>35&&Color.alpha(p)>100)visible++;}check(visible>30,"gallery figure "+i);}
   try(FileOutputStream out=host.openFileOutput("conversation-gallery-"+dims[0]+".png",0)){picture.compress(Bitmap.CompressFormat.PNG,100,out);}picture.recycle();
  }
  for(int i=0;i<6;i++){check(gallery.selected()==i,"gallery next route "+i);gallery.next();}
  check(CharacterPreviewGallery.report(c,2),"local image report");check(!CharacterPreviewGallery.report(c,-1),"invalid report rejected");check(!CharacterPreviewGallery.report(c,6),"out of range report rejected");
  File report=new File(c.getFilesDir(),"character-preview-review.json");String data=new String(java.nio.file.Files.readAllBytes(report.toPath()),"UTF-8");check(data.contains("item")&&!data.contains("prompt")&&!data.contains("question"),"report has no conversation or photos");
  check(CharacterMotionPreferences.saveFrequency(c,0)&&CharacterMotionTuning.frequency==0,"frequency persisted independently");check(CharacterMotionPreferences.saveFrequency(c,2)&&CharacterMotionTuning.frequency==2,"lively frequency independent");SettingsJournal.undo(c);check(CharacterMotionTuning.frequency==0,"frequency undo restores prior setting");CharacterMotionPreferences.saveFrequency(c,1);
  FullBodyMascotView view=new FullBodyMascotView(c);Method safe=FullBodyMascotView.class.getDeclaredMethod("safeDecoded",Bitmap.class,int.class);safe.setAccessible(true);Bitmap blank=Bitmap.createBitmap(512,768,Bitmap.Config.ARGB_8888);check(!(Boolean)safe.invoke(view,blank,4),"transparent image rejected despite valid dimensions");blank.recycle();
  view.setUserContext("ねそべって");view.previewConversationGesture(CharacterGestureLife.JOY,10000);Method posture=FullBodyMascotView.class.getDeclaredMethod("currentVisualPosture");posture.setAccessible(true);check((Integer)posture.invoke(view)==2,"lying reaction preserves posture");
  long before=view.visualRevision();view.setUserContext("立って");check(view.visualRevision()>before,"new command changes revision");check((Integer)posture.invoke(view)==0,"new posture outranks old reaction");view.releaseInspection();
  gallery.close();check(!gallery.ready(),"gallery releases cached images and gestures");gallery.next();check(gallery.selected()==0,"dismissed gallery cannot keep running");assets.close();return n;
 }
}
