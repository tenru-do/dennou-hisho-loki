package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
public final class CharacterGestureLifeCanvasChecks {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 static int hash(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return Arrays.hashCode(p);}
 public static int run(Context c,String source)throws Exception{
  CharacterMotionPreferences.load(c);ok(CharacterMotionPreferences.save(c,2)&&CharacterMotionPreferences.saveIntimacy(c,0),"preferences commit");CharacterMotionTuning.set(0);CharacterMotionTuning.intimacy(3);CharacterMotionPreferences.load(c);ok(CharacterMotionTuning.level==2&&CharacterMotionTuning.intimacy==0,"independent preferences restore");CharacterMotionPreferences.save(c,1);CharacterMotionPreferences.saveIntimacy(c,1);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,source);Resources res=new Resources(assets,c.getResources().getDisplayMetrics(),c.getResources().getConfiguration());Context image=new ContextWrapper(c){public AssetManager getAssets(){return assets;}public Resources getResources(){return res;}};
  Bitmap sheet=Bitmap.createBitmap(240*8,920,Bitmap.Config.ARGB_8888);Canvas sc=new Canvas(sheet);sc.drawColor(Color.BLACK);Set<Integer> faces=new HashSet<>(),bodies=new HashSet<>();
  FullBodyGestureCanvasTest.Host host=new FullBodyGestureCanvasTest.Host(image);FullBodyGestureRenderer body=new FullBodyGestureRenderer(host);host.done=new CountDownLatch(1);body.request(0);ok(host.done.await(15,TimeUnit.SECONDS)&&body.ready(0),"existing body art reused");
  for(int kind=0;kind<8;kind++){
   PortraitExpressionRenderer face=new PortraitExpressionRenderer(image);CharacterRapportPolicy rapport=new CharacterRapportPolicy();face.rapport.life.event(kind,10000);rapport.life.event(kind,10000);body.rapport(rapport,0);int clip=Math.max(0,rapport.bodyClip(11200,false));if(!body.ready(clip)){host.done=new CountDownLatch(1);body.request(clip);ok(host.done.await(15,TimeUnit.SECONDS)&&body.ready(clip),"gesture pose loaded");}
   Bitmap fb=Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_8888),bb=Bitmap.createBitmap(240,600,Bitmap.Config.ARGB_8888);Canvas fc=new Canvas(fb),bc=new Canvas(bb);fc.drawColor(Color.BLACK);bc.drawColor(Color.BLACK);
   ok(face.draw(fc,240,320,0,0,false,false,11200),"face gesture rendered");ok(body.draw(bc,240,600,clip,rapport.bodyTime(11200),11200),"body gesture rendered");faces.add(hash(fb));bodies.add(hash(bb));
   int[] p=new int[240*600];bb.getPixels(p,0,240,0,0,240,600);int bottom=0,count=0;boolean edge=true;for(int y=0;y<600;y++)for(int x=0;x<240;x++)if((p[y*240+x]&0xffffff)!=0){bottom=Math.max(bottom,y);count++;if(x<2||x>=238)edge=false;}ok(count>1500&&bottom>=586&&bottom<=594&&edge,"floor, complete body and side margins retained");
   Bitmap first=Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_8888),second=Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_8888);face.draw(new Canvas(first),240,320,0,0,false,true,11200);face.rapport.life.clear();face.draw(new Canvas(second),240,320,0,0,false,true,11200);ok(hash(first)==hash(second),"navigation quiet suppresses new layer");first.recycle();second.recycle();
   sc.drawBitmap(fb,kind*240,0,null);sc.drawBitmap(bb,kind*240,320,null);fb.recycle();bb.recycle();face.release();
  }
  ok(faces.size()==8,"eight face results distinct");ok(bodies.size()>=6,"body choreography visibly varies");body.release();ok(!body.ready(0),"cache released");try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"gesture-v660-preview.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();return n;
 }
}
