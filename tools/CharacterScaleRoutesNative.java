package com.example.rokidkeyboardbridge;
import android.content.*;import android.graphics.*;import android.view.*;import java.io.*;import java.lang.reflect.*;
/** Actual decoded gesture frames and production fit; no user data, input or camera. */
final class CharacterScaleRoutesNative {
 static int run(Context c,Context host)throws Exception{
  int checks=0,cell=0,page=0;View target=new View(c){public boolean post(Runnable task){task.run();return true;}};
  FullBodyGestureRenderer renderer=new FullBodyGestureRenderer(target);
  Method fit=FullBodyGestureRenderer.class.getDeclaredMethod("fit",int.class,int.class,int.class,int.class);fit.setAccessible(true);
  Field source=FullBodyGestureRenderer.class.getDeclaredField("frames");source.setAccessible(true);
  Bitmap board=Bitmap.createBitmap(960,1152,Bitmap.Config.ARGB_8888),picture=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888);Canvas grid=new Canvas(board),canvas=new Canvas(picture);Paint paint=new Paint(3),text=new Paint(3);text.setColor(Color.WHITE);text.setTextSize(12);
  try{for(int clip=0;clip<44;clip++){
   if(clip>=8&&clip<22)continue;
   renderer.request(clip);long end=android.os.SystemClock.uptimeMillis()+12000;
   while(!renderer.ready(clip)&&android.os.SystemClock.uptimeMillis()<end)Thread.sleep(10);
   if(!renderer.ready(clip))throw new AssertionError("gesture decode "+clip);checks++;
   Bitmap[] frames=((Bitmap[][])source.get(renderer))[clip];
   for(int frame=0;frame<frames.length;frame++){
    Bitmap b=frames[frame];if(SeatedNeutralSource.replaces(clip,frame)){if(b.getWidth()!=262||b.getHeight()!=735)throw new AssertionError("incorrect neutral source dimensions");checks++;}float[] box=(float[])fit.invoke(renderer,clip,frame,480,640);float scale=box[2]/b.getWidth();
    if(Math.abs(scale-box[3]/b.getHeight())>.0001)throw new AssertionError("gesture XY distortion "+clip+"/"+frame);checks++;
    if(Math.abs(scale*(SeatedNeutralSource.replaces(clip,frame)?SeatedNeutralSource.unit():CharacterDistanceCalibration.gestureUnit(clip,frame,b.getHeight()))-26.2897668f)>.002)throw new AssertionError("gesture source anatomy "+clip+"/"+frame);checks++;
    float[] narrow=(float[])fit.invoke(renderer,clip,frame,160,640);
    if(Math.abs(narrow[2]-box[2])>.002)throw new AssertionError("gesture width fitting "+clip+"/"+frame);checks++;
    canvas.drawColor(Color.BLACK);canvas.drawBitmap(b,null,new RectF(box[0],box[1],box[0]+box[2],box[1]+box[3]),paint);
    if(clip<2){
     if(b.getWidth()!=512||b.getHeight()!=768)throw new AssertionError("high-detail greeting resolution "+clip+"/"+frame);checks++;
     if(FullBodyGestureRenderer.sourceFrame(clip,frame)!=frame)throw new AssertionError("greeting return keyframe aliased");checks++;
     if((b.getPixel(0,0)>>>24)!=0||(b.getPixel(511,767)>>>24)!=0)throw new AssertionError("greeting background retained");checks++;
     for(int px=0;px<480;px++)if((picture.getPixel(px,0)&0xffffff)!=0)throw new AssertionError("greeting crown cropped");checks++;
     save(host,picture,"greeting-"+clip+"-"+frame+".png");
    }
    int x=cell%8*120,y=cell/8*192;grid.drawBitmap(picture,null,new Rect(x,y+16,x+120,y+176),paint);grid.drawText(clip+"/"+frame,x+3,y+12,text);cell++;
    if(cell==48){save(host,board,"distance-gestures-"+page+++".png");cell=0;grid.drawColor(Color.BLACK);}
   }
   if(clip<2){
    java.util.Set<Integer> hashes=new java.util.HashSet<>();
    for(int step=0;step<=40;step++){
     canvas.drawColor(Color.BLACK);
     if(!renderer.draw(canvas,480,640,clip,step*100L,10000+clip*10000+step*100L))throw new AssertionError("greeting playback unavailable");checks++;
     int[] pixels=new int[480*640];picture.getPixels(pixels,0,480,0,0,480,640);hashes.add(java.util.Arrays.hashCode(pixels));
     if(clip==1&&step==12){
      float[] placed=(float[])fit.invoke(renderer,1,2,480,640);float factor=placed[2]/512;
      int x=Math.round(placed[0]+235*factor),y=Math.round(placed[1]+375*factor);
      if(Color.red(picture.getPixel(x,y))>=100)throw new AssertionError("raised hand leaves a phantom hand on lap");checks++;
     }
     if(clip==1&&step==6){
      Bitmap expected=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888);Canvas reference=new Canvas(expected);reference.drawColor(Color.BLACK);
      Bitmap neutral=frames[0];float[] placed=(float[])fit.invoke(renderer,1,0,480,640);float factor=placed[2]/neutral.getWidth();
      reference.drawBitmap(neutral,null,new RectF(placed[0],placed[1],placed[0]+placed[2],placed[1]+placed[3]),paint);
      try{for(int sy=155;sy<=180;sy+=5)for(int sx=245;sx<=265;sx+=5){
       int x=Math.round(placed[0]+sx*factor),y=Math.round(placed[1]+sy*factor),actual=picture.getPixel(x,y),wanted=expected.getPixel(x,y);
       if(Math.abs(Color.red(actual)-Color.red(wanted))>18||Math.abs(Color.green(actual)-Color.green(wanted))>18||Math.abs(Color.blue(actual)-Color.blue(wanted))>18)throw new AssertionError("greeting bridge replaces the original neck");checks++;
      }}finally{expected.recycle();}
     }
     save(host,picture,"playback-"+clip+"-"+step+".png");
    }
    if(hashes.size()<4)throw new AssertionError("greeting animation is static");checks++;
   }
  }if(cell>0)save(host,board,"distance-gestures-"+page+".png");}
  finally{renderer.release();board.recycle();picture.recycle();}return checks;
 }
 private static void save(Context host,Bitmap b,String name)throws Exception{try(FileOutputStream out=host.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
