package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.res.*;import android.graphics.*;import java.io.*;
/** Actual decoder/render checks; silhouette proxy is a regression guard, not an anatomy certification. */
public final class StandingAtlasDeviceChecks {
 static int n;static void ok(boolean value,String message){n++;if(!value)throw new AssertionError(message);}
 public static int run(Context system,String apk)throws Exception{
  AssetManager assets=AssetManager.class.getConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,apk);
  Context c=new ContextWrapper(system){public AssetManager getAssets(){return assets;}};
  FullBodyGestureRenderer renderer=new FullBodyGestureRenderer(new FullBodyGestureCanvasTest.Host(c));
  Bitmap preview=Bitmap.createBitmap(240*6,640*4,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(preview);canvas.drawColor(Color.BLACK);
  int[] clips={0,2,4,6,22,24};
  for(int col=0;col<clips.length;col++){
   int clip=clips[col];ok(StandingGestureAtlas.supports(clip),"standing registration");
   Bitmap atlas;try(InputStream in=assets.open(FullBodyGestureRenderer.assetForClip(clip))){atlas=BitmapFactory.decodeStream(in);}
   for(int frame=0;frame<4;frame++){
    int[] b=StandingGestureAtlas.bounds(clip,frame,atlas.getWidth(),atlas.getHeight());
    ok(b[0]>=0&&b[1]>=0&&b[2]<=atlas.getWidth()&&b[3]<=atlas.getHeight(),"source crop bounds");
    Bitmap crop=Bitmap.createBitmap(atlas,b[0],b[1],b[2]-b[0],b[3]-b[1]);
    float head=StandingProportionChecks.head(crop);if((clip==22?frame==3:frame==0))ok(head>=.09f&&head<=.145f,"upper silhouette proxy clip="+clip+" frame="+frame+" ratio="+head);crop.recycle();
   }
   atlas.recycle();renderer.request(clip);long deadline=System.currentTimeMillis()+15000;while(!renderer.ready(clip)&&System.currentTimeMillis()<deadline)Thread.sleep(10);ok(renderer.ready(clip),"decode");
   for(int frame=0;frame<4;frame++){
    renderer.contactPose(frame,0);Bitmap output=Bitmap.createBitmap(240,640,Bitmap.Config.ARGB_8888);Canvas target=new Canvas(output);target.drawColor(Color.BLACK);
    ok(renderer.draw(target,240,640,clip,frame*1300,10000+frame*2000),"standing render");int bottom=0,visible=0;boolean margin=true;
    for(int y=0;y<640;y++)for(int x=0;x<240;x++)if((output.getPixel(x,y)&0xffffff)!=0){visible++;bottom=y;if(x<2||x>=238)margin=false;}
    ok(visible>2000&&bottom>=625&&bottom<=634 ,"portrait clip and floor remain finite "+clip+"/"+frame);canvas.drawBitmap(output,col*240,frame*640,null);output.recycle();
   }
  }
  for(int clip:new int[]{1,3,5,7,23,25})ok(!StandingGestureAtlas.supports(clip),"seated separate");
  for(int clip=8;clip<22;clip+=2)ok(FullBodyGestureRenderer.assetForClip(clip).equals(StandingGestureAtlas.asset(4)),"standing aliases corrected");
  renderer.release();try(FileOutputStream out=new FileOutputStream(new File(system.getFilesDir(),"standing-v685-preview.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,out);}preview.recycle();return n;
 }
}
