package com.example.rokidkeyboardbridge;
import java.io.*;import java.nio.file.*;
public final class ReadPhotoArchiveTest{
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("loki-read-photo-").toFile();byte[] data={1,2,3,4};
  try{PhotoArchive a=new PhotoArchive(dir,8,0);File read=a.saveRead(data,1700000000000L);a.save(data,1700000001000L);a.save(data,1700000002000L);
   if(!read.isFile()||a.pendingReadPhotos().length!=1||a.stats()[2]!=1)throw new AssertionError("READ not protected");
   a=new PhotoArchive(dir,8,0);if(a.pendingReadPhotos().length!=1)throw new AssertionError("pending lost on restart");
   if(ReadPhotoReceiptPolicy.verified(true,true,true,false,false,"id","id","sha","sha",4,4))throw new AssertionError("gallery failure acknowledged");
   if(ReadPhotoReceiptPolicy.verified(true,true,true,false,true,"bad","id","sha","sha",4,4))throw new AssertionError("wrong photo acknowledged");
   if(ReadPhotoReceiptPolicy.verified(true,true,true,false,true,"id","id","bad","sha",4,4))throw new AssertionError("wrong hash acknowledged");
   if(!ReadPhotoReceiptPolicy.verified(true,true,true,false,true,"id","id","sha","sha",4,4))throw new AssertionError("valid receipt rejected");
   if(!ReadPhotoReceiptPolicy.verified(true,true,false,true,false,"id","id","sha","sha",4,4))throw new AssertionError("explicit phone deletion ignored");
   StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(data))hash.append(String.format("%02x",b&255));
   a.markReadCopied(read,hash.toString(),4);if(!read.isFile()||a.pendingReadPhotos().length!=0||a.stats()[2]!=1)throw new AssertionError("copy deleted original/protection");
   System.out.println("PASS READ protection/eviction/restart, gallery-required receipt, wrong ID/hash rejection, phone deletion and original retained after copy");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}
