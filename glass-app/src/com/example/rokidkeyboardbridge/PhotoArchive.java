package com.example.rokidkeyboardbridge;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Bounded archive. Only recorder-owned JPEGs can be evicted; keep markers survive restarts. */
final class PhotoArchive {
 static final long LIMIT=5_000_000_000L,RESERVE=1_000_000_000L,BEFORE=120000,AFTER=60000;
 final File root; final long limit,reserve; private long keepUntil; private PhotoVaultCipher cipher;
 PhotoArchive(File root,PhotoVaultCipher cipher)throws IOException{this(root);this.cipher=cipher;}
 PhotoArchive(File root)throws IOException{this(root,LIMIT,RESERVE);}
 PhotoArchive(File root,long limit,long reserve)throws IOException{
  this.root=root;this.limit=limit;this.reserve=reserve;
  if(!root.isDirectory()&&!root.mkdirs())throw new IOException("写真保存先を作成できません");
  File state=new File(root,"keep-until.txt");if(state.exists())keepUntil=Long.parseLong(new String(Files.readAllBytes(state.toPath()),java.nio.charset.StandardCharsets.US_ASCII).trim());
 }
 private File[] photos(){File[] files=root.listFiles(f->f.isFile()&&f.getName().matches("[0-9]{13}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg(?:\\.gcm)?"));if(files==null)return new File[0];Arrays.sort(files,Comparator.comparing(File::getName));return files;}
 private long time(File f){return Long.parseLong(f.getName().substring(0,13));}
 private File marker(File f){return new File(root,f.getName()+".keep");}
 synchronized long[] stats(){long count=0,bytes=0,kept=0;for(File f:photos()){count++;bytes+=f.length();if(marker(f).exists())kept++;}return new long[]{count,bytes,kept};}
 synchronized int protect(long now)throws IOException{
  keepUntil=Math.max(keepUntil,now+AFTER);
  File temp=new File(root,"keep-until.tmp");Files.write(temp.toPath(),Long.toString(keepUntil).getBytes(java.nio.charset.StandardCharsets.US_ASCII));Files.move(temp.toPath(),new File(root,"keep-until.txt").toPath(),StandardCopyOption.REPLACE_EXISTING);
  int count=0;for(File f:photos())if(time(f)>=now-BEFORE&&time(f)<=now){if(!marker(f).exists()&&!marker(f).createNewFile())throw new IOException("保護に失敗");count++;}return count;
 }
 synchronized File[] entries(){return photos();}
 synchronized byte[] read(File file)throws IOException{
  if(!file.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()))throw new IOException("invalid photo path");
  if(file.length()>8*1024*1024)throw new IOException("photo too large");
  byte[] bytes=Files.readAllBytes(file.toPath());if(!file.getName().endsWith(".gcm"))return bytes;
  try{if(cipher==null)throw new IOException("missing photo key");return cipher.decrypt(bytes);}catch(Exception e){throw new IOException("写真の復号・検証に失敗",e);}
 }
 synchronized void encryptLegacy()throws IOException{
  if(cipher==null)return;
  for(File old:photos())if(!old.getName().endsWith(".gcm")){
   if(Thread.currentThread().isInterrupted())throw new IOException("写真の暗号化を中断しました。元写真を保持します");
   File target=new File(root,old.getName()+".gcm");byte[] plain=read(old);
   if(target.exists()){
    if(!java.util.Arrays.equals(plain,read(target)))throw new IOException("既存写真の暗号化コピーが一致しません");
   }else{
    byte[] encrypted;try{encrypted=cipher.encrypt(plain);}catch(Exception e){throw new IOException("写真の暗号化に失敗",e);}
    if(root.getUsableSpace()<reserve+encrypted.length)throw new IOException("暗号化用の空き容量が不足しています。元写真を保持します");
    File temp=new File(root,target.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(encrypted);out.getFD().sync();}
    Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
   }
   if(marker(old).exists()&&!marker(target).exists()&&!marker(target).createNewFile())throw new IOException("写真の保護を引き継げません");
   if(!old.delete())throw new IOException("元写真を削除できません");if(marker(old).exists())marker(old).delete();
  }
 }
 synchronized File save(byte[] jpeg,long timestamp)throws IOException{
  if(jpeg==null||jpeg.length<4)throw new IOException("撮影データなし");
  if(cipher!=null){try{jpeg=cipher.encrypt(jpeg);}catch(Exception e){throw new IOException("写真の暗号化に失敗",e);}}
  File[] existing=photos();long bytes=0;for(File f:existing)bytes+=f.length();
  for(File f:existing){if(bytes+jpeg.length<=limit&&root.getUsableSpace()>=reserve+jpeg.length)break;
   if(marker(f).exists()||time(f)<=keepUntil&&time(f)>=keepUntil-AFTER-BEFORE)continue;
   long size=f.length();if(!f.delete())throw new IOException("古い写真を削除できません");bytes-=size;
  }
  if(bytes+jpeg.length>limit||root.getUsableSpace()<reserve+jpeg.length)throw new IOException("保存容量不足：保護写真は削除しません");
  String name=String.format(Locale.US,"%013d-%s.jpg",timestamp,UUID.randomUUID())+(cipher==null?"":".gcm");File target=new File(root,name),tmp=new File(root,name+".tmp");
  try(FileOutputStream out=new FileOutputStream(tmp)){out.write(jpeg);out.getFD().sync();}
  if(timestamp<=keepUntil){if(!marker(target).createNewFile())throw new IOException("写真を保護できません");}
  Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);return target;
 }
}
