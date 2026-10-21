package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;import java.util.*;
/** Process-wide exclusive maintenance ownership with durable interruption evidence. */
final class MaintenanceLease {
 static final String PROCESS=UUID.randomUUID().toString();static String active="";
 static synchronized JSONObject status(Context c)throws Exception{JSONObject row=ReviewStore.read(c,"maintenance-lease");if(row.optString("phase").matches("RUNNING|PICKER")&&(!PROCESS.equals(row.optString("process"))||!row.optString("token").equals(active))){row.put("phase","INTERRUPTED").put("endedAt",System.currentTimeMillis());ReviewStore.write(c,"maintenance-lease",row);}return publicStatus(row);}
 static JSONObject publicStatus(JSONObject row)throws Exception{JSONObject v=new JSONObject(row.toString());v.remove("token");v.remove("process");return v;}
 static synchronized String acquire(Context c,String kind)throws Exception{if(!kind.matches("backup|restore|cleanup"))throw new IOException("保守処理を確認してください");status(c);if(!active.isEmpty())throw new IOException("別のバックアップ・復元・整理が進行中です。完了または中止を待ってください");String token=UUID.randomUUID().toString();JSONObject row=new JSONObject().put("token",token).put("process",PROCESS).put("kind",kind).put("phase","RUNNING").put("startedAt",System.currentTimeMillis());ReviewStore.write(c,"maintenance-lease",row);active=token;return token;}
 static synchronized boolean owns(String token){return !token.isEmpty()&&token.equals(active);}
 static synchronized void stage(Context c,String token,String phase)throws Exception{if(!owns(token)||!phase.matches("RUNNING|PICKER"))throw new IOException("保守処理の状態が変わりました");JSONObject row=ReviewStore.read(c,"maintenance-lease");if(!token.equals(row.optString("token")))throw new IOException("保守処理の所有者が変わりました");row.put("phase",phase);ReviewStore.write(c,"maintenance-lease",row);}
 static synchronized void finish(Context c,String token,String phase){if(!owns(token))return;try{if(!phase.matches("COMPLETED|FAILED|CANCELLED|INTERRUPTED"))phase="FAILED";JSONObject row=ReviewStore.read(c,"maintenance-lease");if(token.equals(row.optString("token"))){row.put("phase",phase).put("endedAt",System.currentTimeMillis());ReviewStore.write(c,"maintenance-lease",row);}}catch(Exception ignored){}finally{active="";}}
 private MaintenanceLease(){}
}
