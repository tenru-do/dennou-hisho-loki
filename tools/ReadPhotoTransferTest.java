package com.example.rokidkeyboardbridge;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.KeyGenerator;
import javax.imageio.ImageIO;
import org.json.JSONObject;

/** Synthetic transfer receipts only. No network, gallery, camera, or installed app is touched. */
public final class ReadPhotoTransferTest {
 private static int n;
 private static void check(boolean yes,String label){n++;if(!yes)throw new AssertionError(label);}
 private static byte[] jpeg()throws Exception{
  BufferedImage image=new BufferedImage(768,512,BufferedImage.TYPE_INT_RGB);Random random=new Random(47);
  for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,random.nextInt(1<<24));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",bytes))throw new IOException("fixture JPEG unavailable");return bytes.toByteArray();
 }
 private static JSONObject receipt(JSONObject payload,boolean gallery)throws Exception{return new JSONObject().put("ok",true).put("complete",true).put("stored",true).put("gallerySaved",gallery).put("id",payload.getString("id")).put("sha",payload.getString("sha")).put("total",payload.getInt("total"));}
 private static File save(PhotoArchive archive,byte[] image,long at)throws Exception{ReadPhotoCapture capture=new ReadPhotoCapture();check(capture.save(()->archive,image,at),"fixture READ saved");return capture.savedFile();}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("loki-read-transfer-").toRealPath();
  try {
   byte[] image=jpeg();check(image.length>131072,"real synthetic JPEG spans multiple chunks");long at=1700000000000L;
   KeyGenerator keys=KeyGenerator.getInstance("AES");keys.init(256);PhotoArchive archive=new PhotoArchive(root.toFile(),new PhotoVaultCipher(keys.generateKey()));
   File photo=save(archive,image,at);ByteArrayOutputStream received=new ByteArrayOutputStream();List<Integer> offsets=new ArrayList<>();
   JSONObject success=ReadPhotoTransfer.copy(archive,photo,payload->{
    int offset=payload.getInt("offset");offsets.add(offset);check(payload.getBoolean("autoGallery"),"each READ chunk requests gallery verification");
    check(offset==received.size(),"chunk offsets consecutive");
    check(payload.getInt("total")==image.length,"total is original JPEG byte count");
    if(offset==0)check(payload.getJSONObject("metadata").optBoolean("readOverlay"),"first chunk carries READ provenance");
    else check(!payload.has("metadata"),"metadata not repeated on every chunk");
    received.write(Base64.getDecoder().decode(payload.getString("data")));
    return received.size()==image.length?receipt(payload,true):new JSONObject().put("ok",true).put("complete",false);
   },()->false);
   check(offsets.size()>1&&Arrays.equals(image,received.toByteArray()),"full original arrives unchanged");
   check(success.optBoolean("gallerySaved")&&!ReadPhotoTransfer.pending(photo),"only verified gallery receipt clears READ wait");
   check(photo.isFile()&&Arrays.equals(image,archive.read(photo))&&archive.stats()[2]==1,"confirmed copy retains original and KEEP marker");

   File retry=save(archive,image,at+1);int[] calls={0};
   try {ReadPhotoTransfer.copy(archive,retry,p->{calls[0]++;if(calls[0]==2)throw new IOException("fixture connection lost");return new JSONObject().put("ok",true).put("complete",false);},()->false);throw new AssertionError("failed copy accepted");}catch(IOException expected){}
   check(ReadPhotoTransfer.pending(retry)&&Arrays.equals(image,archive.read(retry)),"partial transfer retains original and pending flag");
   List<Integer> resumed=new ArrayList<>();
   ReadPhotoTransfer.copy(archive,retry,p->{resumed.add(p.getInt("offset"));return receipt(p,true);},()->false);
   check(resumed.equals(Arrays.asList(0)),"existing phone copy still receives chunk zero to run autoGallery");
   check(!ReadPhotoTransfer.pending(retry)&&retry.isFile(),"idempotent retry acknowledges without deleting original");

   for(String bad:new String[]{"gallery","id","sha","total","complete","ok","null"}){
    File pending=save(archive,image,at+2+n);
    boolean rejected=false;
    try {ReadPhotoTransfer.copy(archive,pending,p->{
     if(bad.equals("null"))return null;
     JSONObject r=receipt(p,true);if(bad.equals("gallery"))r.put("gallerySaved",false);
     if(bad.equals("id"))r.put("id","other");if(bad.equals("sha"))r.put("sha","wrong");
     if(bad.equals("total"))r.put("total",image.length-1);if(bad.equals("complete"))r.put("complete",false);if(bad.equals("ok"))r.put("ok",false);return r;
    },()->false);}catch(IOException expected){rejected=true;}
    check(rejected&&ReadPhotoTransfer.pending(pending)&&pending.isFile(),"unverified "+bad+" receipt cannot clear pending or delete source");
   }
   File deleted=save(archive,image,at+200);
   ReadPhotoTransfer.copy(archive,deleted,p->receipt(p,false).put("stored",false).put("deleted",true),()->false);
   check(!ReadPhotoTransfer.pending(deleted)&&deleted.isFile(),"explicit phone deletion is honored without deleting glass original");
   File galleryDeleted=save(archive,image,at+201);
   ReadPhotoTransfer.copy(archive,galleryDeleted,p->receipt(p,false).put("galleryDeleted",true),()->false);
   check(!ReadPhotoTransfer.pending(galleryDeleted)&&galleryDeleted.isFile(),"explicit gallery deletion is honored without recreating it");
   File cancelled=save(archive,image,at+300);int[] sent={0};
   try {ReadPhotoTransfer.copy(archive,cancelled,p->{sent[0]++;return receipt(p,true);},()->true);throw new AssertionError("cancel ignored");}catch(InterruptedIOException expected){}
   check(sent[0]==0&&ReadPhotoTransfer.pending(cancelled)&&cancelled.isFile(),"cancel before copy sends nothing and preserves source");
   AtomicBoolean stop=new AtomicBoolean();
   try {ReadPhotoTransfer.copy(archive,cancelled,p->{sent[0]++;stop.set(true);return new JSONObject().put("ok",true).put("complete",false);},stop::get);throw new AssertionError("mid-copy cancel ignored");}catch(InterruptedIOException expected){}
   check(sent[0]==1&&ReadPhotoTransfer.pending(cancelled)&&cancelled.isFile(),"mid-copy cancel does not send further chunks or acknowledge");
   System.out.println("PASS READ transfer checks="+n+"; synthetic JPEG/receipts only; network/camera/device=0");
  }finally{
   if(!root.startsWith(Paths.get(System.getProperty("java.io.tmpdir")).toRealPath())||!root.getFileName().toString().startsWith("loki-read-transfer-"))throw new IOException("unsafe fixture cleanup");
   try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(path);}
  }
 }
}
