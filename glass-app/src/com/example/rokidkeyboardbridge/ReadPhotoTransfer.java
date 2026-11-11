package com.example.rokidkeyboardbridge;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Shared READ copy path for automatic retries and explicit KEEP/PC imports. Never moves a source. */
final class ReadPhotoTransfer {
 interface Sender {JSONObject send(JSONObject payload)throws Exception;}
 static boolean pending(File file){return new File(file.getParentFile(),file.getName()+".read-pending").isFile();}
 static JSONObject copy(PhotoArchive archive,File file,Sender sender,BooleanSupplier cancelled)throws Exception {
  if(cancelled.getAsBoolean())throw new InterruptedIOException("READ写真の取り込みを中断しました");
  if(!file.getName().matches("[0-9]{13}-[a-f0-9-]{36}\\.jpg(?:\\.gcm)?"))throw new IOException("invalid READ source");
  byte[] jpeg=archive.read(file);
  if(jpeg.length<4||jpeg.length>8000000)throw new IOException("READ写真のサイズを確認できません");
  String id=file.getName().substring(0,50);StringBuilder hash=new StringBuilder();
  for(byte value:MessageDigest.getInstance("SHA-256").digest(jpeg))hash.append(String.format(Locale.US,"%02x",value&255));
  String sha=hash.toString();JSONObject receipt=null;
  for(int offset=0;offset<jpeg.length;offset+=131072){
   if(cancelled.getAsBoolean())throw new InterruptedIOException("READ写真の取り込みを中断しました");
   JSONObject payload=new JSONObject().put("id",id).put("sha",sha).put("offset",offset)
    .put("total",jpeg.length).put("autoGallery",true)
    .put("data",Base64.getEncoder().encodeToString(Arrays.copyOfRange(jpeg,offset,Math.min(jpeg.length,offset+131072))));
   if(offset==0)payload.put("metadata",new JSONObject(new String(archive.metadataForTransfer(file),StandardCharsets.UTF_8)));
   // Always send the first chunk, including an existing phone copy, so its gallery receipt is checked.
   receipt=sender.send(payload);
   if(receipt!=null&&receipt.optBoolean("complete"))break;
  }
  if(receipt==null||!ReadPhotoReceiptPolicy.verified(receipt.optBoolean("ok"),receipt.optBoolean("complete"),
    receipt.optBoolean("stored"),receipt.optBoolean("deleted")||receipt.optBoolean("galleryDeleted"),
    receipt.optBoolean("gallerySaved"),receipt.optString("id"),id,receipt.optString("sha"),sha,
    receipt.optInt("total",-1),jpeg.length))throw new IOException("READ写真のスマホ・ギャラリー保存を確認できません");
  archive.markReadCopied(file,sha,jpeg.length);
  return receipt;
 }
 private ReadPhotoTransfer(){}
}
