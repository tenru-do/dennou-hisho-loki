package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.crypto.KeyGenerator;
import org.json.JSONObject;

/** Self-target instrumentation entry; writes only synthetic data below the caller's cache directory. */
public final class ReadPhotoCaptureChecks {
 private static int count;
 private static void check(boolean yes,String label){count++;if(!yes)throw new AssertionError(label);}
 public static int run(Context context)throws Exception{
  count=0;Path cache=context.getCacheDir().getCanonicalFile().toPath();
  Path root=Files.createTempDirectory(cache,"read-capture-checks-").toRealPath();
  try {
   Bitmap bitmap=Bitmap.createBitmap(96,64,Bitmap.Config.ARGB_8888);
   Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint();paint.setColor(Color.BLUE);canvas.drawRect(4,4,88,58,paint);
   ByteArrayOutputStream encoded=new ByteArrayOutputStream();check(bitmap.compress(Bitmap.CompressFormat.JPEG,90,encoded),"synthetic Android JPEG encoded");bitmap.recycle();byte[] original=encoded.toByteArray();
   KeyGenerator keys=KeyGenerator.getInstance("AES");keys.init(256);PhotoVaultCipher cipher=new PhotoVaultCipher(keys.generateKey());
   PhotoArchive archive=new PhotoArchive(root.resolve("encrypted").toFile(),cipher);ReadPhotoCapture capture=new ReadPhotoCapture();int[] opens={0};long at=1700000000000L;
   check(capture.save(()->{opens[0]++;return archive;},original,at),"native JPEG locally archived");File photo=capture.savedFile();
   check(photo.isFile()&&photo.getName().endsWith(".gcm"),"native source encrypted");
   check(Arrays.equals(original,archive.read(photo)),"raw native JPEG preserved byte-for-byte");
   Bitmap decoded=BitmapFactory.decodeByteArray(archive.read(photo),0,original.length);check(decoded!=null&&decoded.getWidth()==96&&decoded.getHeight()==64,"saved native JPEG decodes");decoded.recycle();
   check(capture.metadataSaved()&&ReadPhotoTransfer.pending(photo)&&archive.stats()[2]==1,"metadata/protection/pending recorded");
   JSONObject metadata=new JSONObject(new String(archive.metadata(photo),StandardCharsets.UTF_8));check(metadata.optBoolean("readOverlay")&&!metadata.optBoolean("available"),"minimum metadata does not read actual location");
   for(int retry=0;retry<3;retry++)check(capture.save(()->{opens[0]++;return archive;},original,at),"AI retry uses existing capture receipt");
   check(opens[0]==1&&archive.entries().length==1,"AI retries cannot duplicate local photo");
   ReadPhotoCapture separate=new ReadPhotoCapture();check(separate.save(()->archive,original,at+1)&&archive.entries().length==2,"new intentional capture remains separate");
   File meta=new File(archive.root,photo.getName()+".meta.gcm");check(meta.delete(),"remove only synthetic metadata fixture");
   check(new JSONObject(new String(archive.metadataForTransfer(photo),StandardCharsets.UTF_8)).optBoolean("readOverlay"),"missing metadata preserves READ identity");
   int[] sent={0};
   ReadPhotoTransfer.copy(archive,photo,payload->{sent[0]++;check(payload.optBoolean("autoGallery")&&payload.getInt("offset")==0,"first payload requests gallery receipt even if already stored");check(Arrays.equals(original,Base64.getDecoder().decode(payload.getString("data"))),"transport uses original native JPEG");return new JSONObject().put("ok",true).put("complete",true).put("stored",true).put("gallerySaved",true).put("id",payload.getString("id")).put("sha",payload.getString("sha")).put("total",payload.getInt("total"));},()->false);
   check(sent[0]==1&&!ReadPhotoTransfer.pending(photo)&&photo.isFile(),"verified native copy clears pending but retains glass original");
   PhotoArchive reopened=new PhotoArchive(archive.root,cipher);check(reopened.pendingReadPhotos().length==1&&reopened.stats()[2]==2,"native restart preserves remaining wait/protection");
   ReadPhotoCapture unavailable=new ReadPhotoCapture();check(!unavailable.save(()->{throw new IOException("synthetic storage unavailable");},original,at),"archive unavailable is fail-open");check(unavailable.status().contains("読み取りは続けます")&&!unavailable.failureSuffix().contains("保存済み"),"native failed save never claims stored");
   PhotoArchive full=new PhotoArchive(root.resolve("full").toFile(),original.length*2L,0);File first=full.save(original,at),second=full.save(original,at+1);
   ReadPhotoCapture failed=new ReadPhotoCapture();check(!failed.save(()->full,original,at+2),"native full archive fails without stopping caller");check(first.isFile()&&second.isFile()&&full.entries().length==2,"native READ does not evict older originals");
   PhotoArchive plain=new PhotoArchive(root.resolve("no-key").toFile(),original.length*2L,0);ReadPhotoCapture partial=new ReadPhotoCapture();check(partial.save(()->plain,original,at)&&!partial.metadataSaved(),"metadata failure does not discard native source");check(Arrays.equals(original,plain.read(partial.savedFile()))&&ReadPhotoTransfer.pending(partial.savedFile()),"metadata failure still permits original review/retry");
   return count;
  }finally{
   if(!root.startsWith(cache)||!root.getFileName().toString().startsWith("read-capture-checks-"))throw new IOException("fixture cleanup escaped own cache");
   try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(path);}
  }
 }
}
