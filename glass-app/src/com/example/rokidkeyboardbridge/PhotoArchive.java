package com.example.rokidkeyboardbridge;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Bounded archive. Only recorder-owned JPEGs can be evicted; keep markers survive restarts. */
final class PhotoArchive {
 static final long LIMIT=5_000_000_000L,RESERVE=1_000_000_000L,BEFORE=120000,AFTER=60000;
 final File root; final long limit,reserve; private long keepUntil;
 PhotoArchive(File root)throws IOException{this(root,LIMIT,RESERVE);}
 PhotoArchive(File root,long limit,long reserve)throws IOException{
  this.root=root;this.limit=limit;this.reserve=reserve;
  if(!root.isDirectory()&&!root.mkdirs())throw new IOException("写真保存先を作成できません");
  File state=new File(root,"keep-until.txt");if(state.exists())keepUntil=Long.parseLong(new String(Files.readAllBytes(state.toPath()),java.nio.charset.StandardCharsets.US_ASCII).trim());
 }
 private File[] photos(){File[] files=root.listFiles(f->f.getName().matches("[0-9]{13}-[a-f0-9-]+\\.jpg"));if(files==null)return new File[0];Arrays.sort(files,Comparator.comparing(File::getName));return files;}
 private long time(File f){return Long.parseLong(f.getName().substring(0,13));}
 private File marker(File f){return new File(root,f.getName()+".keep");}
 synchronized long[] stats(){long count=0,bytes=0,kept=0;for(File f:photos()){count++;bytes+=f.length();if(marker(f).exists())kept++;}return new long[]{count,bytes,kept};}
 synchronized int protect(long now)throws IOException{
  keepUntil=Math.max(keepUntil,now+AFTER);
  File temp=new File(root,"keep-until.tmp");Files.write(temp.toPath(),Long.toString(keepUntil).getBytes(java.nio.charset.StandardCharsets.US_ASCII));Files.move(temp.toPath(),new File(root,"keep-until.txt").toPath(),StandardCopyOption.REPLACE_EXISTING);
  int count=0;for(File f:photos())if(time(f)>=now-BEFORE&&time(f)<=now){if(!marker(f).exists()&&!marker(f).createNewFile())throw new IOException("保護に失敗");count++;}return count;
 }
 synchronized File save(byte[] jpeg,long timestamp)throws IOException{
  if(jpeg==null||jpeg.length<4)throw new IOException("撮影データなし");
  File[] existing=photos();long bytes=0;for(File f:existing)bytes+=f.length();
  for(File f:existing){if(bytes+jpeg.length<=limit&&root.getUsableSpace()>=reserve+jpeg.length)break;
   if(marker(f).exists()||time(f)<=keepUntil&&time(f)>=keepUntil-AFTER-BEFORE)continue;
   long size=f.length();if(!f.delete())throw new IOException("古い写真を削除できません");bytes-=size;
  }
  if(bytes+jpeg.length>limit||root.getUsableSpace()<reserve+jpeg.length)throw new IOException("保存容量不足：保護写真は削除しません");
  String name=String.format(Locale.US,"%013d-%s.jpg",timestamp,UUID.randomUUID());File target=new File(root,name),tmp=new File(root,name+".tmp");
  try(FileOutputStream out=new FileOutputStream(tmp)){out.write(jpeg);out.getFD().sync();}
  if(timestamp<=keepUntil){if(!marker(target).createNewFile())throw new IOException("写真を保護できません");}
  Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);return target;
 }
}
