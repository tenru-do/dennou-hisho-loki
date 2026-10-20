package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Private encrypted text drafts. This store has no networking or execution queue. */
final class OfflineNotes {
 static final int LIMIT=100,MAX_TEXT=2800;
 static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"offline-notes-v1.gcm"));}
 static synchronized JSONArray read(Context c)throws Exception{
  try{if(file(c).getBaseFile().length()>2L*1024*1024)throw new IOException("メモの保存サイズが上限を超えています");return new JSONArray(new String(DeviceSecrets.photos().decrypt(file(c).readFully()),StandardCharsets.UTF_8));}
  catch(FileNotFoundException absent){return new JSONArray();}
 }
 private static void write(Context c,JSONArray rows)throws Exception{
  AtomicFile f=file(c);FileOutputStream out=null;
  try{out=f.startWrite();out.write(DeviceSecrets.photos().encrypt(rows.toString().getBytes(StandardCharsets.UTF_8)));f.finishWrite(out);}
  catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
 }
 static synchronized String add(Context c,String text)throws Exception{
  if(text==null||text.trim().isEmpty()||text.length()>MAX_TEXT)throw new IOException("1〜2800文字の本文を入力してください");
  JSONArray rows=read(c);if(rows.length()>=LIMIT)throw new IOException("メモが100件あります。不要なメモを手動削除してください");
  String id=java.util.UUID.randomUUID().toString();rows.put(new JSONObject().put("id",id).put("at",System.currentTimeMillis()).put("text",text).put("state","LOCAL_ONLY"));write(c,rows);return id;
 }
 static synchronized boolean remove(Context c,String id)throws Exception{
  JSONArray rows=read(c),next=new JSONArray();boolean found=false;
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(id.equals(row.optString("id")))found=true;else next.put(row);}
  if(found)write(c,next);return found;
 }
 private OfflineNotes(){}
}
