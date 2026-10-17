package com.example.rokidgeminisecretary;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.*;
import java.security.*;

/** Deterministic READ name plus pending-row recovery prevents duplicate gallery copies. */
final class ReadGalleryWriter {
 static final class GalleryDeletedException extends IOException {GalleryDeletedException(){super("gallery photo was deleted");}}
 static synchronized Uri save(Context c,File photo)throws Exception{
  if(!photo.getName().matches("[0-9]{13}-[a-f0-9-]{36}\\.gcm")||!photo.getCanonicalFile().getParentFile().equals(RecorderPhotoStore.root(c).getCanonicalFile()))throw new IOException("invalid READ photo");
  if(Build.VERSION.SDK_INT<29&&c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=0)throw new IOException("ギャラリー保存のストレージ権限が必要です");
  String name="Loki-READ-"+photo.getName().replace(".gcm",".jpg"),directory=android.os.Environment.DIRECTORY_PICTURES+"/Loki/READ/";
  SharedPreferences prefs=c.getSharedPreferences("recorder_gallery_exports",Context.MODE_PRIVATE);
  Uri uri=null;String previous=prefs.getString(photo.getName(),"");
  if(!previous.isEmpty()){
   Uri candidate=Uri.parse(previous);try(Cursor row=c.getContentResolver().query(candidate,new String[]{"_id"},null,null,null)){if(row==null)throw new IOException("gallery query unavailable");if(!row.moveToFirst())throw new GalleryDeletedException();}
   try(android.content.res.AssetFileDescriptor a=c.getContentResolver().openAssetFileDescriptor(candidate,"r")){if(a!=null&&a.getLength()!=0)return candidate;throw new IOException("gallery photo is empty");}
  }
  Uri collection=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
  if(Build.VERSION.SDK_INT>=29){
   Uri query=MediaStore.setIncludePending(collection);
   try(Cursor rows=c.getContentResolver().query(query,new String[]{"_id","is_pending"},"_display_name=? AND relative_path=? AND owner_package_name=?",new String[]{name,directory,c.getPackageName()},"_id DESC")){
    if(rows!=null&&rows.moveToFirst()){uri=ContentUris.withAppendedId(collection,rows.getLong(0));if(rows.getInt(1)==0){if(!prefs.edit().putString(photo.getName(),uri.toString()).commit())throw new IOException("gallery receipt failed");return uri;}}
   }
  }
  if(uri==null){ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,name);values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");values.put(MediaStore.Images.Media.DATE_TAKEN,Long.parseLong(photo.getName().substring(0,13)));if(Build.VERSION.SDK_INT>=29){values.put(MediaStore.Images.Media.RELATIVE_PATH,directory);values.put(MediaStore.Images.Media.IS_PENDING,1);}uri=c.getContentResolver().insert(collection,values);if(uri==null)throw new IOException("gallery insert failed");}
  final Uri target=uri;
  try{
   MessageDigest expected=MessageDigest.getInstance("SHA-256");
   try(OutputStream raw=c.getContentResolver().openOutputStream(target,"w")){if(raw==null)throw new IOException("gallery output failed");try(DigestOutputStream out=new DigestOutputStream(raw,expected)){RecorderGalleryActivity.exportJpeg(c,photo,out);}}
   MessageDigest actual=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(target)){if(in==null)throw new IOException("gallery readback failed");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)actual.update(b,0,n);}
   if(!MessageDigest.isEqual(expected.digest(),actual.digest()))throw new IOException("gallery hash mismatch");
   if(Build.VERSION.SDK_INT>=29){ContentValues ready=new ContentValues();ready.put(MediaStore.Images.Media.IS_PENDING,0);if(c.getContentResolver().update(target,ready,null,null)!=1)throw new IOException("gallery publish failed");}
   if(!prefs.edit().putString(photo.getName(),target.toString()).commit())throw new IOException("gallery receipt failed");return target;
  }catch(Exception failure){
   // Only remove a still-hidden partial row. A published image survives a receipt failure.
   if(Build.VERSION.SDK_INT>=29)try(Cursor row=c.getContentResolver().query(target,new String[]{"is_pending"},null,null,null)){if(row!=null&&row.moveToFirst()&&row.getInt(0)==1)c.getContentResolver().delete(target,null,null);}catch(Exception ignored){}
   throw failure;
  }
 }
}
