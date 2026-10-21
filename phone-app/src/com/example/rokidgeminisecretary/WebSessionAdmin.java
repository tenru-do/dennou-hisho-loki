package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
/** Phone-only administration. Public handles are not bearer tokens or token hashes. */
final class WebSessionAdmin {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("loki_web_console",0);}
 static String handle(JSONObject s)throws Exception{return RecorderPhotoStore.hash(("local-session-admin:"+s.getString("hash")).getBytes("UTF-8"));}
 static String revision(JSONObject s)throws Exception{return ReviewStore.hash(new JSONObject().put("expires",s.getLong("expires")).put("scope",s.optString("scope","full")).put("name",s.optString("name")));}
 static synchronized JSONArray list(Context c)throws Exception{JSONArray raw=new JSONArray(prefs(c).getString("sessions","[]")),out=new JSONArray();for(int i=0;i<raw.length();i++){JSONObject s=raw.getJSONObject(i);if(s.optLong("expires")>System.currentTimeMillis())out.put(new JSONObject().put("id",handle(s)).put("revision",revision(s)).put("name",s.optString("name")).put("scope",s.optString("scope","full")).put("createdAt",s.optLong("createdAt")).put("lastUsedAt",s.optLong("lastUsedAt")).put("expires",s.getLong("expires")));}return out;}
 static void change(Context c,JSONObject input)throws Exception{LokiWebConsole.changeSession(c,input);}
 private WebSessionAdmin(){}
}
