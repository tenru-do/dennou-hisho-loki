package com.example.rokidgeminisecretary;
import android.content.*;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
final class HealbeHydrationAlert {
 static boolean matches(String pkg,String text){
  if(!"com.healbe.healbegobe".equals(pkg)||text==null)return false;
  String s=text.toLowerCase(Locale.ROOT);
  return s.contains("水分補給")||s.contains("水を飲")||s.contains("水分が不足")||s.contains("drink water")||s.contains("time to drink")||s.contains("low hydration")||s.contains("dehydration");
 }
 static boolean fresh(long at,long now){return at>0&&now-at>=0&&now-at<=300000;}
 static String fingerprint(String text)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:hash)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
 static synchronized void forward(Context context,long at,String pkg,String title,String body){
  String message=(title+"\n"+body).trim();long now=System.currentTimeMillis();
  if(!matches(pkg,message)||!fresh(at,now))return;
  try{
   if(message.length()>500)message=message.substring(0,500);
   SharedPreferences prefs=context.getSharedPreferences("healbe_alert_delivery",0);
   String hash=fingerprint(message);long last=prefs.getLong("lastAt",0);
   if(hash.equals(prefs.getString("lastHash",""))&&now-last>=0&&now-last<600000)return;
   String id=fingerprint(pkg+"|"+at+"|"+message);
   JSONObject alert=new JSONObject().put("id",id).put("issuedAt",at).put("message",message).put("kind","status");
   // Append without replacing user commands; notification text never becomes an AI prompt.
   if(MainActivity.queueBridgeCommand(context,"__LOKI_HEALBE_ALERT__:"+alert,true))prefs.edit().putString("lastHash",hash).putLong("lastAt",now).apply();
  }catch(Exception e){android.util.Log.w("LokiHEALBE","Hydration notification forwarding unavailable");}
 }
}
