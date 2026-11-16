package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.pm.*;import android.content.res.*;import android.graphics.*;import java.io.*;import java.util.zip.*;
/** Production standing renderer, synthetic clocks and isolated app storage only. */
public final class FullBodyIdleDeviceChecks {
 public static int run(Context host)throws Exception{
  File apk=new File(host.getFilesDir(),"glass.apk");AssetManager assets=AssetManager.class.getDeclaredConstructor().newInstance();AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,apk.getPath());ApplicationInfo info=new ApplicationInfo(host.getApplicationInfo());info.sourceDir=info.publicSourceDir=apk.getPath();
  Context c=new ContextWrapper(host){public AssetManager getAssets(){return assets;}public ApplicationInfo getApplicationInfo(){return info;}};
  FullBodyMascotView view=new FullBodyMascotView(c);view.layout(0,0,480,640);CharacterRenderBudget.reset();CharacterChoreography.update(false,false,false,false,false,false,10000);
  if(CharacterChoreography.scene!=0||CharacterChoreography.gain!=1)throw new AssertionError("ordinary idle reduced");int n=1,poses=0;
  Bitmap a=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888);int[] x=new int[480*640],y=new int[x.length];
  try(ZipFile z=new ZipFile(apk)){
   for(int pose=0;pose<168&&poses<3;pose++){
    if(FullBodyMotionPolicy.isSeated(pose))continue;Bitmap source=FullBodyRealisticAssets.decode(z,pose,1);
    Canvas ca=new Canvas(a),cb=new Canvas(b);ca.drawColor(Color.BLACK);cb.drawColor(Color.BLACK);
    view.renderInspection(ca,source,pose,1100,false,true,null,-1,0);view.renderInspection(cb,source,pose,2700,false,true,null,-1,0);
    a.getPixels(x,0,480,0,0,480,640);b.getPixels(y,0,480,0,0,480,640);int changed=0;for(int i=180*480;i<x.length;i++)if(x[i]!=y[i])changed++;
    if(changed<100)throw new AssertionError("standing body static "+pose+" changed="+changed);n++;
    if(poses==0){try(FileOutputStream f=new FileOutputStream(new File(host.getFilesDir(),"standing-idle-a.png"))){a.compress(Bitmap.CompressFormat.PNG,100,f);}try(FileOutputStream f=new FileOutputStream(new File(host.getFilesDir(),"standing-idle-b.png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}}
    poses++;source.recycle();
   }
  }finally{view.releaseInspection();a.recycle();b.recycle();}
  if(poses!=3)throw new AssertionError("missing standing samples");return n;
 }
}
