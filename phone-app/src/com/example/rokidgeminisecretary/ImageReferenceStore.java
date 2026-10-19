package com.example.rokidgeminisecretary;
import android.content.Context;import android.util.AtomicFile;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;
/** Encrypted local copy of the glasses' recent reading, without image bytes/location. */
final class ImageReferenceStore {
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"image-reference-summary.gcm"));}
 private static JSONObject raw(Context c)throws Exception{return new JSONObject(new String(DeviceSecrets.photos().decrypt(file(c).readFully()),StandardCharsets.UTF_8));}
 static synchronized void accept(Context c,JSONObject input)throws Exception{
  JSONObject next=ImageReferenceSummary.checked(input);long previous=0;try{previous=raw(c).optLong("at");}catch(FileNotFoundException absent){}
  if(next.getLong("at")<previous)return;
  AtomicFile f=file(c);FileOutputStream out=null;try{out=f.startWrite();out.write(DeviceSecrets.photos().encrypt(next.toString().getBytes(StandardCharsets.UTF_8)));f.finishWrite(out);}catch(Exception error){if(out!=null)f.failWrite(out);throw error;}
 }
 static synchronized JSONObject view(Context c){try{JSONObject result=ImageReferenceSummary.view(raw(c),System.currentTimeMillis());if(!result.optBoolean("available"))file(c).delete();return result;}catch(Exception unavailable){return new JSONObject();}}
 private ImageReferenceStore(){}
}
