package com.example.rokidkeyboardbridge;

import java.io.File;
import java.nio.charset.StandardCharsets;

/** One camera capture owns one local save attempt, independent of the AI request/retries. */
final class ReadPhotoCapture {
 interface ArchiveSource { PhotoArchive get() throws Exception; }
 private boolean attempted,metadataSaved;
 private PhotoArchive archive;
 private File photo;

 synchronized boolean save(ArchiveSource source,byte[] original,long capturedAt){
  if(attempted)return saved();
  attempted=true;
  try {
   archive=source.get();
   photo=archive.saveRead(original,capturedAt);
   // Preserve READ classification even if location lookup or richer metadata later fails.
   metadata(("{\"capturedAt\":"+capturedAt+",\"available\":false,\"readOverlay\":true}").getBytes(StandardCharsets.UTF_8));
  }catch(Exception unavailable){
   // A failed optional archive must not discard a successfully captured image or stop analysis.
  }
  return saved();
 }

 synchronized boolean metadata(byte[] value){
  if(!saved())return false;
  try {archive.metadata(photo,value);metadataSaved=true;return true;}
  catch(Exception unavailable){metadataSaved=false;return false;}
 }

 synchronized boolean saved(){return photo!=null&&photo.isFile();}
 synchronized boolean metadataSaved(){return saved()&&metadataSaved;}
 synchronized File savedFile(){return photo;}
 synchronized String status(){
  return saved()?"READ写真をグラスに保存。スマホ・PCへの取り込み待ち"
      :"READ写真を保存できませんでした。読み取りは続けます。空き容量を確認してください";
 }
 synchronized String failureSuffix(){
  return saved()?" 撮影原本はグラスに保存済みです。スマホ・PCへの取り込みは保存状況を確認してください。"
      :attempted?" 撮影原本の保存は確認できません。空き容量を確認してください。":"";
 }
}
