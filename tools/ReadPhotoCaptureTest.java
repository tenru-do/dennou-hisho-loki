package com.example.rokidkeyboardbridge;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.KeyGenerator;
import javax.imageio.ImageIO;
import org.json.JSONObject;

/** Host-only synthetic JPEGs: no camera, device, real photo, Android secrets, or network. */
public final class ReadPhotoCaptureTest {
 private static int checks;
 private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private static String sha(byte[] bytes)throws Exception{
  StringBuilder hash=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(Locale.US,"%02x",value&255));return hash.toString();
 }
 private static byte[] jpeg()throws Exception{
  BufferedImage image=new BufferedImage(64,48,BufferedImage.TYPE_INT_RGB);
  for(int y=0;y<48;y++)for(int x=0;x<64;x++)image.setRGB(x,y,((x*4)<<16)|((y*5)<<8)|0x3f);
  ByteArrayOutputStream out=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",out))throw new IOException("fixture JPEG encoder unavailable");return out.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  Path testRoot=Files.createTempDirectory("loki-read-save-").toRealPath();
  try {
   byte[] source=jpeg(),untouched=source.clone();long at=1700000000000L;
   KeyGenerator keys=KeyGenerator.getInstance("AES");keys.init(256);PhotoVaultCipher cipher=new PhotoVaultCipher(keys.generateKey());
   PhotoArchive archive=new PhotoArchive(testRoot.resolve("encrypted").toFile(),cipher);
   ReadPhotoCapture capture=new ReadPhotoCapture();int[] opens={0};
   ReadPhotoCapture.ArchiveSource store=()->{opens[0]++;return archive;};
   check(capture.failureSuffix().isEmpty(),"no save claim before capture");
   check(capture.save(store,source,at),"successful camera JPEG archived");
   File saved=capture.savedFile();
   check(saved.getName().endsWith(".gcm"),"production path encrypts original");
   check(!Arrays.equals(Files.readAllBytes(saved.toPath()),source),"disk does not contain plaintext JPEG");
   check(Arrays.equals(archive.read(saved),source),"raw camera JPEG roundtrip, no overlay/recompression");
   check(Arrays.equals(untouched,source),"capture bytes remain unchanged for AI normalization");
   check(capture.metadataSaved(),"minimum provenance saved independently of location");
   JSONObject minimum=new JSONObject(new String(archive.metadata(saved),StandardCharsets.UTF_8));
   check(minimum.getBoolean("readOverlay")&&!minimum.getBoolean("available")&&minimum.getLong("capturedAt")==at,"minimum READ metadata never fabricates location");
   check(archive.pendingReadPhotos().length==1&&archive.stats()[2]==1,"READ protected and pending immediately");
   for(int retry=0;retry<3;retry++)check(capture.save(store,source,at),"same capture AI retry keeps first receipt");
   check(opens[0]==1&&archive.entries().length==1&&saved.equals(capture.savedFile()),"save source opened once, no duplicate files");
   check(capture.status().contains("グラスに保存")&&capture.status().contains("取り込み待ち"),"local storage never claimed as phone/PC completion");
   check(capture.failureSuffix().contains("撮影原本はグラスに保存済み"),"AI error can retain correct local save receipt");
   JSONObject context=ReadCaptureContext.safe(null,at);
   check(capture.metadata(context.toString().getBytes(StandardCharsets.UTF_8)),"richer metadata update supported");
   check(Arrays.equals(archive.read(saved),source),"metadata update cannot alter raw JPEG");
   ReadPhotoCapture second=new ReadPhotoCapture();
   check(second.save(()->archive,source,at),"separate deliberate capture accepted");
   check(archive.entries().length==2&&!saved.equals(second.savedFile()),"same-looking new capture is not incorrectly deduplicated");
   PhotoArchive reopened=new PhotoArchive(archive.root,cipher);
   check(reopened.pendingReadPhotos().length==2&&reopened.stats()[2]==2,"protection and transfer wait survive process restart");
   reopened.markReadCopied(saved,sha(source),source.length);
   check(saved.isFile()&&Arrays.equals(reopened.read(saved),source),"phone receipt retains local original");
   check(reopened.pendingReadPhotos().length==1&&reopened.stats()[2]==2,"receipt only clears transfer wait, not KEEP protection");

   File missing=new File(archive.root,second.savedFile().getName()+".meta.gcm");
   check(missing.delete(),"remove own synthetic metadata for missing-sidecar fixture");
   JSONObject missingMeta=new JSONObject(new String(archive.metadataForTransfer(second.savedFile()),StandardCharsets.UTF_8));
   check(missingMeta.optBoolean("readOverlay")&&!missingMeta.optBoolean("available"),"missing metadata keeps pending READ identity without invented position");
   check(second.metadata(context.toString().getBytes(StandardCharsets.UTF_8)),"restore own fixture metadata");
   byte[] damaged=Files.readAllBytes(missing.toPath());damaged[damaged.length-1]^=1;Files.write(missing.toPath(),damaged);
   JSONObject damagedMeta=new JSONObject(new String(archive.metadataForTransfer(second.savedFile()),StandardCharsets.UTF_8));
   check(damagedMeta.optBoolean("readOverlay")&&damagedMeta.optBoolean("metadataUnavailable"),"damaged metadata retains READ classification during safe fallback");
   check(Arrays.equals(damaged,Files.readAllBytes(missing.toPath())),"unreadable metadata original remains untouched");
   check(Arrays.equals(damaged,Files.readAllBytes(new File(missing.getPath()+".recovery").toPath())),"unreadable metadata backed up byte-for-byte");
   check(Arrays.equals(archive.read(second.savedFile()),source),"metadata failure cannot discard authentic JPEG");

   ReadPhotoCapture noMetadata=new ReadPhotoCapture();PhotoArchive plain=new PhotoArchive(testRoot.resolve("plain").toFile(),source.length*4L,0);
   check(noMetadata.save(()->plain,source,at+1),"metadata key failure does not stop local image save");
   check(noMetadata.saved()&&!noMetadata.metadataSaved(),"photo receipt and metadata result distinguished");
   check(!noMetadata.metadata(context.toString().getBytes(StandardCharsets.UTF_8)),"later metadata failure is fail-open");
   check(Arrays.equals(plain.read(noMetadata.savedFile()),source),"photo still available after metadata failure");
   check(plain.pendingReadPhotos().length==1,"metadata failure retains transfer opportunity");
   File notRead=plain.save(source,at+2);
   check(!new JSONObject(new String(plain.metadataForTransfer(notRead),StandardCharsets.UTF_8)).optBoolean("readOverlay"),"ordinary KEEP is not mislabeled READ");

   PhotoArchive full=new PhotoArchive(testRoot.resolve("full").toFile(),source.length*2L,0);
   File older=full.save(source,at-2),newer=full.save(source,at-1);
   ReadPhotoCapture noSpace=new ReadPhotoCapture();
   check(!noSpace.save(()->full,source,at),"full archive reports failed optional save without throwing");
   check(older.isFile()&&newer.isFile()&&full.entries().length==2,"READ save never evicts older originals");
   check(Arrays.equals(full.read(older),source)&&Arrays.equals(full.read(newer),source),"full-archive existing bytes preserved");
   check(!noSpace.saved()&&noSpace.savedFile()==null&&!noSpace.metadataSaved(),"failed save cannot claim a receipt");
   check(noSpace.status().contains("保存できません")&&noSpace.status().contains("読み取りは続けます"),"save failure and analysis continuation clearly separated");
   check(!noSpace.failureSuffix().contains("保存済み"),"AI failure never claims an unsaved original");
   check(newer.delete(),"free only own synthetic fixture file");
   check(!noSpace.save(()->full,source,at)&&full.entries().length==1,"AI retry never retries or duplicates failed optional save");

   ReadPhotoCapture unavailable=new ReadPhotoCapture();int[] failedOpens={0};
   check(!unavailable.save(()->{failedOpens[0]++;throw new IOException("synthetic unavailable storage");},source,at),"missing storage source contained");
   check(!unavailable.save(()->{failedOpens[0]++;return archive;},source,at)&&failedOpens[0]==1,"unavailable source is attempted only once per capture");
   check(!unavailable.metadata(context.toString().getBytes(StandardCharsets.UTF_8)),"no metadata writes without image receipt");
   check(!new ReadPhotoCapture().save(()->archive,null,at),"missing capture data cannot report saved");
   System.out.println("PASS READ photo capture checks="+checks+"; synthetic JPEG only; camera/API/device=0");
  }finally{
   Path configuredTemp=Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
   if(!testRoot.startsWith(configuredTemp)||!testRoot.getFileName().toString().startsWith("loki-read-save-"))throw new IOException("fixture cleanup escaped intended temporary directory");
   try(java.util.stream.Stream<Path> paths=Files.walk(testRoot)){for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(path);}
  }
 }
}
