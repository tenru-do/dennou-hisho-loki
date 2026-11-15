package com.example.rokidkeyboardbridge;
import android.graphics.*;import android.view.View;import java.io.*;import java.util.zip.*;
/** One lazy half-resolution bitmap, existing host ticker, no network or sensors. */
final class RecoveryExerciseRenderer {
 final View host;private Bitmap bitmap;private int loaded=-1,wanted=-1,ticket;private boolean loading;private long retry;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private final float[] mesh=new float[17*33*2];
 RecoveryExerciseRenderer(View host){this.host=host;}
 Bitmap decode(int id)throws Exception{try(ZipFile z=new ZipFile(host.getContext().getApplicationInfo().sourceDir)){ZipEntry e=z.getEntry("assets/"+RecoveryExercisePolicy.asset(id));if(e==null)throw new IOException("missing recovery asset");try(InputStream in=z.getInputStream(e)){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;Bitmap b=BitmapFactory.decodeStream(in,null,o);if(b==null)throw new IOException("invalid recovery asset");return b;}}}
 void request(int id){if(!RecoveryExercisePolicy.valid(id))return;wanted=id;if(loaded==id||loading||android.os.SystemClock.uptimeMillis()<retry)return;loading=true;int t=ticket;new Thread(()->{Bitmap next=null;try{next=decode(id);}catch(Exception ignored){}final Bitmap b=next;host.post(()->{if(t!=ticket||wanted!=id){if(b!=null)b.recycle();if(t==ticket){loading=false;request(wanted);}return;}loading=false;if(b==null){retry=android.os.SystemClock.uptimeMillis()+3000;return;}Bitmap old=bitmap;bitmap=b;loaded=id;if(old!=null)old.recycle();host.invalidate();});},"LokiRecoveryDecode").start();}
 boolean draw(Canvas c,int w,int h,int id,long now,int strength,boolean animate){request(id);if(bitmap==null||loaded!=id)return false;drawBitmap(c,bitmap,w,h,id,now,animate?strength:0);return true;}
 void drawBitmap(Canvas c,Bitmap b,int w,int h,int id,long now,int strength){float[] box=RecoveryExercisePolicy.box(id,w,h);int save=c.save();c.translate(box[0],box[1]);c.scale(box[2]/b.getWidth(),box[3]/b.getHeight());if(strength==0)c.drawBitmap(b,0,0,paint);else{RecoveryExercisePolicy.mesh(mesh,b.getWidth(),b.getHeight(),RecoveryExercisePolicy.faceBottom(id),now,strength);c.drawBitmapMesh(b,16,32,mesh,0,null,0,paint);}c.restoreToCount(save);}
 void release(){ticket++;wanted=loaded=-1;loading=false;retry=0;if(bitmap!=null){bitmap.recycle();bitmap=null;}}
}
