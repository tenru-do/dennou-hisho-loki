package com.example.rokidgeminisecretary;
import android.content.*;import java.io.*;import org.json.*;import java.util.*;
/** Persistent, explicit requests to remove a selected photo from both devices. */
final class PhotoDeletionSync {
 static boolean valid(String id){return id!=null&&id.matches("[0-9]{13}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("photo_glass_deletions",0);}
 static synchronized void enqueue(Context c,String id)throws IOException{if(!valid(id))throw new IOException("invalid deletion id");if(!prefs(c).edit().putBoolean(id,true).commit())throw new IOException("deletion queue not saved");}
 static synchronized JSONObject pending(Context c)throws Exception{JSONArray ids=new JSONArray();int total=0;TreeSet<String> keys=new TreeSet<>(prefs(c).getAll().keySet());for(String id:keys)if(valid(id)&&prefs(c).getBoolean(id,false)){total++;if(ids.length()<100)ids.put(id);}return new JSONObject().put("ok",true).put("ids",ids).put("pending",total);}
 static synchronized JSONObject acknowledge(Context c,JSONArray ids)throws Exception{if(ids.length()>100)throw new IOException("deletion batch too large");for(int i=0;i<ids.length();i++)if(!valid(ids.getString(i)))throw new IOException("invalid deletion id");SharedPreferences.Editor e=prefs(c).edit();for(int i=0;i<ids.length();i++)e.remove(ids.getString(i));if(!e.commit())throw new IOException("deletion acknowledgment not saved");return pending(c);}
}
