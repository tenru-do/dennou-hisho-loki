package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.view.*;import java.io.*;
public final class RecoveryExerciseDeviceChecks {
 static int checks;static void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static int run(Context owner)throws Exception{
  checks=0;Context c=new ContextWrapper(owner){public ApplicationInfo getApplicationInfo(){ApplicationInfo a=new ApplicationInfo(super.getApplicationInfo());a.sourceDir=new File(owner.getFilesDir(),"glass.apk").getPath();return a;}};
  RecoveryExerciseRenderer renderer=new RecoveryExerciseRenderer(new View(c));
  for(int id=0;id<RecoveryExercisePolicy.COUNT;id++){
   Bitmap source=renderer.decode(id);ok(source!=null&&source.getWidth()>100,"actual packaged artwork decodes");
   float[] box=RecoveryExercisePolicy.box(id,480,640);ok(box.length==4&&box[2]>0&&box[3]>0,"valid same-camera box");
   Bitmap still=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888),strong=Bitmap.createBitmap(480,640,Bitmap.Config.ARGB_8888);Canvas a=new Canvas(still),b=new Canvas(strong);a.drawColor(Color.BLACK);b.drawColor(Color.BLACK);renderer.drawBitmap(a,source,480,640,id,675,0);renderer.drawBitmap(b,source,480,640,id,675,2);
   int visible=0,difference=0;for(int y=0;y<640;y+=4)for(int x=0;x<480;x+=4){int p=still.getPixel(x,y);if((p&0xffffff)>0x202020)visible++;if(p!=strong.getPixel(x,y))difference++;}ok(visible>60,"visible pixels reach native canvas");ok(difference>0,"strong breathing changes actual pixels");
   float[] mesh=new float[17*33*2];RecoveryExercisePolicy.mesh(mesh,512,768,RecoveryExercisePolicy.faceBottom(id),675,2);for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){int at=(row*17+col)*2;ok(Math.abs(mesh[at]-512*col/16f)<.0001,"no lateral/body-width distortion");float y=row/32f;if(y<=RecoveryExercisePolicy.faceBottom(id)||y>=.7f)ok(Math.abs(mesh[at+1]-768*y)<.0001,"face and lower-body anchored");}
   if(id==0||id==8||id==16){try(FileOutputStream f=new FileOutputStream(new File(owner.getFilesDir(),"recovery-"+id+".png"))){strong.compress(Bitmap.CompressFormat.PNG,100,f);}}
   source.recycle();still.recycle();strong.recycle();
  }
  renderer.release();return checks;
 }
}
