package com.example.rokidkeyboardbridge;
import java.io.*;
import java.nio.file.*;
public final class PhotoArchiveTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 private static void retainedPhotoChecks(File parent,byte[] jpeg)throws IOException{
  File archive=new File(parent,"inventory");
  check(PhotoArchive.retainedCount(archive)==0&&!archive.exists(),"empty inventory does not create an archive");
  check(archive.mkdir(),"fixture archive directory");
  String id="1700000000000-00000000-0000-0000-0000-000000000001.jpg";
  String other="1700000000001-00000000-0000-0000-0000-000000000002.jpg";
  try{
   for(String name:new String[]{id+".gcm.tmp",id+".meta.gcm",id+".gcm.keep",id+".gcm.read-pending",id+".gcm.phone-deleted","user.jpg"}){
    Files.write(archive.toPath().resolve(name),jpeg);
   }
   check(PhotoArchive.retainedCount(archive)==0,"tmp, sidecars and unrelated JPEG are not retained photos");
   Path plain=Files.write(archive.toPath().resolve(id),jpeg);
   Path encrypted=Files.write(archive.toPath().resolve(id+".gcm"),jpeg);
   check(PhotoArchive.retainedCount(archive)==1,"plain and encrypted copies of same original count once");
   Path second=Files.write(archive.toPath().resolve(other+".gcm"),jpeg);
   check(PhotoArchive.retainedCount(archive)==2,"distinct completed JPEGs count");
   Files.delete(plain);
   Files.delete(encrypted);
   check(PhotoArchive.retainedCount(archive)==1,"count reflects deletion independent of capture history");
   Files.delete(second);
   check(PhotoArchive.retainedCount(archive)==0,"zero retained despite temporary and sidecar files");
   File unreadable=new File(archive.getPath()){@Override public File[] listFiles(java.io.FileFilter filter){return null;}};
   boolean rejected=false;try{PhotoArchive.retainedCount(unreadable);}catch(IllegalStateException e){rejected=true;}
   check(rejected,"unreadable inventory is not zero retained photos");
  }finally{for(File file:archive.listFiles())file.delete();archive.delete();}
 }
 public static void main(String[]args)throws Exception{
  check(PhotoArchive.LIMIT==5_000_000_000L,"5GB archive limit");
  File dir=Files.createTempDirectory("loki-archive-test").toFile();byte[] jpeg={1,2,3,4};long now=1700000000000L;
  try{
   retainedPhotoChecks(dir,jpeg);
   File deletionRoot=new File(dir,"delete-sync");check(deletionRoot.mkdir(),"deletion fixture");
   try{
    PhotoArchive deletionArchive=new PhotoArchive(deletionRoot,1000,0);
    File deleted=deletionArchive.save(jpeg,now),preserved=deletionArchive.save(jpeg,now+1);
    String id=deleted.getName().substring(0,50);
    for(String suffix:new String[]{".meta.gcm",".meta.gcm.recovery",".keep",".read-pending",".phone-deleted"})Files.write(new File(deletionRoot,deleted.getName()+suffix).toPath(),jpeg);
    Files.write(new File(deletionRoot,id+".jpg.gcm").toPath(),jpeg);
    boolean bad=false;try{deletionArchive.removeAcknowledgedDeletion("../"+id);}catch(IOException e){bad=true;}
    check(bad&&deleted.exists()&&preserved.exists(),"invalid ID never deletes files");
    deletionArchive.removeAcknowledgedDeletion(id);
    check(!deleted.exists()&&preserved.exists()&&deletionRoot.listFiles().length==1,"only acknowledged original, duplicate and sidecars removed");
    deletionArchive.removeAcknowledgedDeletion(id);
    check(preserved.exists(),"retry after lost acknowledgment is idempotent");
   }finally{for(File f:deletionRoot.listFiles())f.delete();deletionRoot.delete();}
   PhotoArchive a=new PhotoArchive(dir,12,0);File first=a.save(jpeg,now-300000),second=a.save(jpeg,now-200000);a.save(jpeg,now-100000);
   File unrelated=new File(dir,"user.jpg");Files.write(unrelated.toPath(),jpeg);
   a.save(jpeg,now-50000);check(!first.exists()&&second.exists(),"oldest first");check(unrelated.exists(),"unrelated preserved");
   check(a.protect(now)==2,"last two minutes only");check(second.exists(),"older photo not protected");
   a=new PhotoArchive(dir,12,0);a.save(jpeg,now+5000);check(!second.exists(),"unprotected older evicted");check(a.stats()[2]==3,"future protection survives restart");
   boolean full=false;try{a.save(jpeg,now+70000);}catch(IOException e){full=true;}check(full&&a.stats()[0]==3,"all protected never evicted");
   check(PhotoRecorderCommands.action("今の場面を残して。")==1,"keep command");check(PhotoRecorderCommands.action("連続撮影を開始してください")==2,"start command");check(PhotoRecorderCommands.action("ドラレコを止めて")==3,"stop command");
   for(String s:new String[]{"場面を保存する方法は？","今の場面を残さないで","彼が今の場面を残してと言った","思い出を保存して"})check(PhotoRecorderCommands.action(s)==0,"not a command: "+s);
   File blocked=new File(dir.getPath()){@Override public File[] listFiles(java.io.FileFilter filter){return null;}};
   boolean unavailable=false;try{new PhotoArchive(blocked,12,0).entries();}catch(IllegalStateException e){unavailable=true;}
   check(unavailable,"unreadable directory never means zero photos");
   File emptyDir=Files.createTempDirectory("loki-low-space-").toFile();
   try{File noSpace=new File(emptyDir.getPath()){@Override public long getUsableSpace(){return 0;}};
    PhotoArchive blockedArchive=new PhotoArchive(noSpace,100,1);boolean low=false;try{blockedArchive.preflightCapture();}catch(IOException e){low=true;}
    check(low && emptyDir.listFiles().length==0,"low-space capture rejected before camera without allocating storage");
    File synthetic=new File(emptyDir,"1700000000000-00000000-0000-0000-0000-000000000001.jpg");Files.write(synthetic.toPath(),jpeg);
    blockedArchive.preflightCapture();check(synthetic.exists(),"preflight preserves existing eviction behavior without deleting originals");synthetic.delete();
   }finally{emptyDir.delete();}
   System.out.println("PASS current photo inventory, temporary/sidecar exclusion, deletion and dedup, protective eviction and voice exclusions");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}
