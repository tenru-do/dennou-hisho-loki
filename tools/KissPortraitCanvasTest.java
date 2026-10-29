package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import java.io.*;import java.util.*;
public final class KissPortraitCanvasTest {
 static int checks;static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static int run(Context system,String source)throws Exception{
  int previous=FullBodyGestureCanvasTest.run(system,source);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,source);Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());
  Context context=new ContextWrapper(system){public AssetManager getAssets(){return assets;}public Resources getResources(){return resources;}};
  KissPortraitRenderer renderer=new KissPortraitRenderer(context);ok(renderer.ready(),"kiss atlas ready");Set<Integer> hashes=new HashSet<>();Bitmap sheet=Bitmap.createBitmap(2240,640,Bitmap.Config.ARGB_8888);Canvas page=new Canvas(sheet);page.drawColor(Color.BLACK);
  for(int phase=0;phase<2;phase++)for(int type=0;type<7;type++){
   Bitmap b=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.BLACK);long t=phase==0?Math.min(100,KissGesturePolicy.LEAD[type]/2):KissGesturePolicy.LEAD[type]+100;renderer.scene(type,t);
   ok(renderer.draw(canvas,320,320,0,false,10000),"kiss phase render");int[] pixels=new int[320*320];b.getPixels(pixels,0,320,0,0,320,320);hashes.add(Arrays.hashCode(pixels));boolean clear=true;for(int y=0;y<320;y++)for(int x=0;x<3;x++)if((pixels[y*320+x]&0xffffff)!=0||(pixels[y*320+319-x]&0xffffff)!=0)clear=false;ok(type<4||clear,"portrait fits strong motion viewport type="+type+" phase="+phase);page.drawBitmap(b,type*320,phase*320,null);b.recycle();
  }
  ok(hashes.size()==14,"four base and three strong variants");renderer.scene(1,1200);ok(!renderer.draw(new Canvas(Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)),100,100,1,false,10000),"listening preempts");ok(!renderer.draw(new Canvas(Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)),100,100,0,true,10000),"navigation/recording quiet");
  try(FileOutputStream out=new FileOutputStream(new File(system.getFilesDir(),"kiss-preview.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();renderer.release();return previous+checks;
 }
}
