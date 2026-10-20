package com.example.rokidgeminisecretary;
import android.util.AtomicFile;import java.io.*;
/** Atomic encrypted draft file; callers prepare/validate all data before replacing it. */
final class ImageDraftStore {
 static final long MAX_BYTES=16L*1024*1024;
 static void write(File target,byte[] plain)throws Exception{byte[] encrypted=DeviceSecrets.photos().encrypt(plain);if(encrypted.length>MAX_BYTES)throw new IOException("添付の下書きが大きすぎます");AtomicFile file=new AtomicFile(target);FileOutputStream out=null;try{out=file.startWrite();out.write(encrypted);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
 static byte[] read(File target)throws Exception{AtomicFile file=new AtomicFile(target);if(file.getBaseFile().length()>MAX_BYTES)throw new IOException("添付の下書きが大きすぎます");try(InputStream in=file.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[16384];int count;long total=0;while((count=in.read(buffer))!=-1){total+=count;if(total>MAX_BYTES)throw new IOException("添付の下書きが大きすぎます");out.write(buffer,0,count);}return DeviceSecrets.photos().decrypt(out.toByteArray());}}
 static void delete(File target){new AtomicFile(target).delete();}
 private ImageDraftStore(){}
}
