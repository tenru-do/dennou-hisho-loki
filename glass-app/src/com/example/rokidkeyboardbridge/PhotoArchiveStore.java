package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.IOException;

/** One archive instance serializes migration, capture and explicit export in this process. */
final class PhotoArchiveStore {
 private static PhotoArchive instance;
 static synchronized PhotoArchive get(Context context)throws Exception{
  if(instance==null)instance=new PhotoArchive(directory(context),DeviceSecrets.photos());
  return instance;
 }
 static int retainedCount(Context context)throws IOException{
  File directory=directory(context);
  String state=Environment.getExternalStorageState(directory.getParentFile());
  if(!Environment.MEDIA_MOUNTED.equals(state)&&!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
   throw new IOException("写真保存先が利用できません");
  }
  return PhotoArchive.retainedCount(directory);
 }
 private static File directory(Context context)throws IOException{
  File pictures=context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
  if(pictures==null)throw new IOException("写真保存先が利用できません");
  return new File(pictures,"loki-recorder");
 }
}
