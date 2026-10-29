package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import android.view.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
/** Synthetic offscreen keyframe decode and Canvas playback; no camera or live user state. */
public final class FullBodyGestureCanvasTest {
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static final class Host extends View {CountDownLatch done=new CountDownLatch(1);Host(Context c){super(c);}@Override public boolean post(Runnable r){r.run();done.countDown();return true;}}
 public static int run(Context system,String source)throws Exception{
  int previous=CharacterLifeCanvasTest.run(system,source);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,source);
  Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());Context context=new ContextWrapper(system){public AssetManager getAssets(){return assets;}public Resources getResources(){return resources;}};
  Host host=new Host(context);FullBodyGestureRenderer renderer=new FullBodyGestureRenderer(host);
  Bitmap sheet=Bitmap.createBitmap(240*11,600*2,Bitmap.Config.ARGB_8888);Canvas montage=new Canvas(sheet);montage.drawColor(Color.BLACK);
  Bitmap correction=Bitmap.createBitmap(720,600,Bitmap.Config.ARGB_8888);Canvas corrected=new Canvas(correction);corrected.drawColor(Color.BLACK);
  for(int clip=0;clip<22;clip++){
   if(!renderer.ready(clip)){host.done=new CountDownLatch(1);renderer.request(clip);ok(host.done.await(15,TimeUnit.SECONDS),"bounded decode "+clip);}ok(renderer.ready(clip),"registered clip "+clip);
   Set<Integer> hashes=new HashSet<>();
   for(int time:new int[]{0,600,1100,1500,2300,4200}){
    Bitmap bitmap=Bitmap.createBitmap(240,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.drawColor(Color.BLACK);ok(renderer.draw(c,240,600,clip,time,10000),"render keyframe");
    int[] pixels=new int[240*600];bitmap.getPixels(pixels,0,240,0,0,240,600);hashes.add(Arrays.hashCode(pixels));if(clip==5&&(time==0||time==600||time==1500))corrected.drawBitmap(bitmap,time==0?0:time==600?240:480,0,null);int bottom=-1,visible=0;
    for(int i=0;i<pixels.length;i++)if((pixels[i]&0xffffff)>0){bottom=Math.max(bottom,i/240);visible++;}
    ok(visible>2000,"visible figure");boolean edgeClear=true;for(int y=0;y<600;y++)for(int x=0;x<4;x++)if((pixels[y*240+x]&0xffffff)!=0||(pixels[y*240+239-x]&0xffffff)!=0)edgeClear=false;/* Overflow is intentionally cropped; the viewport itself is checked separately. */ok(bottom>=586&&bottom<=594,"shared floor anchor clip="+clip+" time="+time+" bottom="+bottom);
    if((clip<8&&time==1500)||(clip>=8&&time==(((clip-8)/2==0||(clip-8)/2==2)?600:1100)))montage.drawBitmap(bitmap,(clip/2)*240,(clip%2)*600,null);bitmap.recycle();
   }
   ok(hashes.size()>=3,"visible movement across clip "+clip);
  }
  renderer.release();for(int c=0;c<22;c++)ok(!renderer.ready(c),"lifecycle releases keyframes");
  try(FileOutputStream output=new FileOutputStream(new File(system.getFilesDir(),"preview.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,output);}sheet.recycle();
  try(FileOutputStream output=new FileOutputStream(new File(system.getFilesDir(),"seated-corrected-preview.png"))){correction.compress(Bitmap.CompressFormat.PNG,100,output);}correction.recycle();
  return previous+checks;
 }
}
