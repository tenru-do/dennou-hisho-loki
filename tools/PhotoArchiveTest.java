package com.example.rokidkeyboardbridge;
import java.io.*;
import java.nio.file.*;
public final class PhotoArchiveTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  check(PhotoArchive.LIMIT==5_000_000_000L,"5GB archive limit");
  File dir=Files.createTempDirectory("loki-archive-test").toFile();byte[] jpeg={1,2,3,4};long now=1700000000000L;
  try{
   PhotoArchive a=new PhotoArchive(dir,12,0);File first=a.save(jpeg,now-300000),second=a.save(jpeg,now-200000);a.save(jpeg,now-100000);
   File unrelated=new File(dir,"user.jpg");Files.write(unrelated.toPath(),jpeg);
   a.save(jpeg,now-50000);check(!first.exists()&&second.exists(),"oldest first");check(unrelated.exists(),"unrelated preserved");
   check(a.protect(now)==2,"last two minutes only");check(second.exists(),"older photo not protected");
   a=new PhotoArchive(dir,12,0);a.save(jpeg,now+5000);check(!second.exists(),"unprotected older evicted");check(a.stats()[2]==3,"future protection survives restart");
   boolean full=false;try{a.save(jpeg,now+70000);}catch(IOException e){full=true;}check(full&&a.stats()[0]==3,"all protected never evicted");
   check(PhotoRecorderCommands.action("今の場面を残して。")==1,"keep command");check(PhotoRecorderCommands.action("連続撮影を開始してください")==2,"start command");check(PhotoRecorderCommands.action("ドラレコを止めて")==3,"stop command");
   for(String s:new String[]{"場面を保存する方法は？","今の場面を残さないで","彼が今の場面を残してと言った","思い出を保存して"})check(PhotoRecorderCommands.action(s)==0,"not a command: "+s);
   System.out.println("PASS oldest-only eviction, unrelated preservation, before/after protection, restart, full protected archive stop, voice command exclusions");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}
