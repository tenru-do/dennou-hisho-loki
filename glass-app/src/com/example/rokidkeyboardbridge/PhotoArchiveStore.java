package com.example.rokidkeyboardbridge;
/** One archive instance serializes migration, capture and explicit export in this process. */
final class PhotoArchiveStore {
 private static PhotoArchive instance;
 static synchronized PhotoArchive get(android.content.Context context)throws Exception{
  if(instance==null){java.io.File directory=context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);if(directory==null)throw new java.io.IOException("写真保存先が利用できません");instance=new PhotoArchive(new java.io.File(directory,"loki-recorder"),DeviceSecrets.photos());}return instance;
 }
}
