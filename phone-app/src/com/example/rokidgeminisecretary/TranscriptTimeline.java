package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;import java.nio.file.*;
final class TranscriptTimeline {
 static JSONArray read(Context c,String id)throws Exception{File folder=MeetingAudioStore.folder(c,id);JSONObject meta=MeetingAudioStore.meta(folder);JSONArray parts=meta.getJSONArray("parts"),out=new JSONArray();long at=0;for(int first=0;first<parts.length();first+=6){long start=at;for(int j=first;j<Math.min(first+6,parts.length());j++)at+=parts.getJSONObject(j).getLong("bytes")/32;File f=new File(folder,String.format(java.util.Locale.US,"transcript-%04d.gcm",first));if(!f.isFile())continue;if(f.length()>2500000)throw new IOException("文字起こし区間が大きすぎます");String text=new String(DeviceSecrets.photos().decrypt(Files.readAllBytes(f.toPath())),"UTF-8");out.put(new JSONObject().put("startMs",start).put("endMs",at).put("text",text).put("approximate",true));}return out;}
 private TranscriptTimeline(){}
}
