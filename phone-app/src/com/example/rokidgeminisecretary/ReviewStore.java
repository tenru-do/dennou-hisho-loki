package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
/** Bounded encrypted local documents. A missing file and an unreadable file are distinct. */
final class ReviewStore {
 static JSONObject read(Context c,String name)throws Exception{if(!name.matches("[a-z0-9-]{1,80}"))throw new IOException("保存名が不正です");try{return ConversationWorkspace.read(c,"reliability-"+name);}catch(FileNotFoundException absent){return new JSONObject();}}
 static void write(Context c,String name,JSONObject v)throws Exception{if(!name.matches("[a-z0-9-]{1,80}")||v.toString().length()>1500000)throw new IOException("保存上限です。元の記録は保持しています");ConversationWorkspace.write(c,"reliability-"+name,v);}
 static String hash(JSONObject v)throws Exception{return RecorderPhotoStore.hash(v.toString().getBytes("UTF-8"));}
 static void validId(String id)throws IOException{if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IOException("操作IDを確認してください");}
 static void history(JSONObject row)throws Exception{JSONArray old=row.optJSONArray("history"),next=new JSONArray();if(old!=null)for(int i=Math.max(0,old.length()-9);i<old.length();i++)next.put(old.get(i));JSONObject copy=new JSONObject();for(String key:new String[]{"revision","text","title","request","conditions","date_text","state","manualCompleted","updatedAt"})if(row.has(key))copy.put(key,row.get(key));copy.put("savedAt",System.currentTimeMillis());next.put(copy);row.put("history",next);}
 private ReviewStore(){}
}
