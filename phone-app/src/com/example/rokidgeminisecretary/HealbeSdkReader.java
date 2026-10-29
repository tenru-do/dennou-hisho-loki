package com.example.rokidgeminisecretary;
import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.net.Uri;import org.json.*;import java.time.*;
final class HealbeSdkReader {
 static final String PACKAGE="com.example.lokihealbe";
 static JSONObject read(Context c){try{
  if(c.getPackageManager().checkSignatures(c.getPackageName(),PACKAGE)!=PackageManager.SIGNATURE_MATCH)return new JSONObject();
  try(Cursor cursor=c.getContentResolver().query(Uri.parse("content://com.example.lokihealbe.energy/latest"),new String[]{"json"},null,null,null)){
   if(cursor==null||!cursor.moveToFirst())return new JSONObject();String raw=cursor.getString(0);if(raw==null||raw.length()>4096)return new JSONObject();JSONObject v=new JSONObject(raw);
   int balance=v.optInt("balanceKcal",Integer.MIN_VALUE);long measured=v.optLong("measuredAt"),seen=v.optLong("observedAt"),now=System.currentTimeMillis();
   if(!"HEALBE_SDK".equals(v.optString("source"))||balance< -15000||balance>15000||measured<=0||seen<measured||seen>now)return new JSONObject();
   String day=Instant.ofEpochMilli(measured).atZone(ZoneId.systemDefault()).toLocalDate().toString();if(!day.equals(v.optString("date")))return new JSONObject();
   return new JSONObject().put("balanceKcal",balance).put("measuredAt",measured).put("observedAt",seen).put("date",day).put("source","HEALBE_SDK").put("provisional",true);
  }
 }catch(Exception unavailable){return new JSONObject();}}
 static JSONObject preferred(Context c,JSONObject screen){JSONObject sdk=read(c);if(!sdk.has("balanceKcal"))return screen;long age=System.currentTimeMillis()-sdk.optLong("measuredAt");return age>=0&&age<=900000||!screen.has("balanceKcal")?sdk:screen;}
 static void open(android.app.Activity c){try{c.startActivity(new Intent().setClassName(PACKAGE,PACKAGE+".MainActivity"));}catch(Exception missing){new android.app.AlertDialog.Builder(c).setMessage("HEALBE直接連携の補助アプリをインストールすると、このボタンからログインできます。").setPositiveButton("閉じる",null).show();}}
}
