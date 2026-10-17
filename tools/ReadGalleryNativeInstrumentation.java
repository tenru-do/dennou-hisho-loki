package com.example.lokisecuritytest;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.net.Uri;import android.database.Cursor;import android.provider.MediaStore;import org.json.*;import java.io.*;import java.lang.reflect.*;import java.util.*;import java.security.*;
/** Synthetic fixture only: verifies MediaStore publication/retry/recovery, then removes its own rows. */
public final class ReadGalleryNativeInstrumentation extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private Object invoke(Class<?> owner,String name,Class<?>[] types,Object... values)throws Exception{Method m=owner.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,values);}
 private void check(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 public void onStart(){Bundle result=new Bundle();Context c=getTargetContext();String id=System.currentTimeMillis()+"-"+UUID.randomUUID(),name="Loki-READ-"+id+".jpg";File photo=null;String sha="";boolean success=false;
  try{
   ClassLoader loader=c.getClassLoader();Class<?> store=Class.forName("com.example.rokidgeminisecretary.RecorderPhotoStore",true,loader),writer=Class.forName("com.example.rokidgeminisecretary.ReadGalleryWriter",true,loader);
   Bitmap bitmap=Bitmap.createBitmap(96,64,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint();paint.setColor(Color.BLACK);paint.setTextSize(12);canvas.drawText("LOKI READ TEST",2,30,paint);ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes);bitmap.recycle();byte[] jpeg=bytes.toByteArray();StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(jpeg))hash.append(String.format(Locale.US,"%02x",b&255));sha=hash.toString();
   JSONObject packet=new JSONObject().put("id",id).put("sha",sha).put("offset",0).put("total",jpeg.length).put("data",android.util.Base64.encodeToString(jpeg,2)).put("metadata",new JSONObject().put("available",false));
   JSONObject receipt=(JSONObject)invoke(store,"accept",new Class[]{Context.class,JSONObject.class},c,packet);check(receipt.optBoolean("stored")&&receipt.optBoolean("complete"),"encrypted phone receipt");
   File root=(File)invoke(store,"root",new Class[]{Context.class},c);photo=new File(root,id+".gcm");check(Arrays.equals(jpeg,(byte[])invoke(store,"read",new Class[]{File.class},photo)),"encrypted image roundtrip");
   Uri first=(Uri)invoke(writer,"save",new Class[]{Context.class,File.class},c,photo),again=(Uri)invoke(writer,"save",new Class[]{Context.class,File.class},c,photo);check(first.equals(again),"duplicate URI");
   try(Cursor row=c.getContentResolver().query(first,new String[]{"is_pending","relative_path"},null,null,null)){check(row!=null&&row.moveToFirst()&&row.getInt(0)==0&&"Pictures/Loki/READ/".equals(row.getString(1)),"gallery published/path");}
   try(InputStream in=c.getContentResolver().openInputStream(first)){Bitmap restored=BitmapFactory.decodeStream(in);check(restored!=null&&restored.getWidth()==96&&restored.getHeight()==64,"gallery dimensions");restored.recycle();}
   SharedPreferences prefs=c.getSharedPreferences("recorder_gallery_exports",0);prefs.edit().remove(photo.getName()).commit();Uri recovered=(Uri)invoke(writer,"save",new Class[]{Context.class,File.class},c,photo);check(first.equals(recovered),"published row recovery without receipt");
   c.getContentResolver().delete(first,null,null);prefs.edit().remove(photo.getName()).commit();ContentValues values=new ContentValues();values.put("_display_name",name);values.put("mime_type","image/jpeg");values.put("relative_path","Pictures/Loki/READ/");values.put("is_pending",1);Uri partial=c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);check(partial!=null,"pending fixture insert");Uri repaired=(Uri)invoke(writer,"save",new Class[]{Context.class,File.class},c,photo);check(partial.equals(repaired),"pending row recovery");
   c.getContentResolver().delete(repaired,null,null);boolean deletion=false;try{invoke(writer,"save",new Class[]{Context.class,File.class},c,photo);}catch(InvocationTargetException deleted){deletion=deleted.getCause().getClass().getSimpleName().equals("GalleryDeletedException");}check(deletion&&photo.exists(),"honor gallery deletion; private original retained");
   result.putString("galleryNative","PASS encrypted source, published READ album, decoded pixels, duplicate retry, lost receipt recovery, pending recovery, explicit deletion");success=true;
  }catch(Throwable failure){Throwable cause=failure instanceof InvocationTargetException?failure.getCause():failure;result.putString("failure",cause.getClass().getSimpleName()+": "+cause.getMessage());}
  finally{
   try{ArrayList<Uri> own=new ArrayList<>();try(Cursor rows=c.getContentResolver().query(MediaStore.setIncludePending(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),new String[]{"_id"},"_display_name=? AND owner_package_name=?",new String[]{name,c.getPackageName()},null)){if(rows!=null)while(rows.moveToNext())own.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,rows.getLong(0)));}for(Uri uri:own)c.getContentResolver().delete(uri,null,null);
    if(photo!=null){photo.delete();new File(photo.getParentFile(),id+".meta.gcm").delete();new File(photo.getParentFile(),id+"-"+sha+"-0.part").delete();c.getSharedPreferences("recorder_gallery_exports",0).edit().remove(photo.getName()).commit();}result.putString("fixtureCleanup","PASS");
   }catch(Throwable cleanup){result.putString("fixtureCleanup","FAIL");success=false;}
  }finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }
}
