package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.time.*;
final class HealbeEnergyStore {
 static synchronized void observe(Context c,String text,String source,long at){
  Integer balance=HealbeEnergyText.balance(text);long now=System.currentTimeMillis();
  if(balance==null||at<=0||now<at||now-at>300000||!source.matches("HEALBE_SCREEN|HEALBE_NOTIFICATION"))return;
  if(text.matches("(?is).*(昨日|一昨日|yesterday|履歴|history).*"))return;
  if(source.equals("HEALBE_SCREEN")&&!HealbeEnergyText.todayScreen(text))return;
  if(!Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().equals(LocalDate.now()))return;
  try{ReviewStore.write(c,"healbe-energy",new JSONObject().put("balanceKcal",balance).put("observedAt",at).put("date",LocalDate.now().toString()).put("source",source).put("provisional",true));}
  catch(Exception e){android.util.Log.w("LokiHEALBE","Energy cache could not be saved");}
 }
 static JSONObject snapshot(Context c){try{return ReviewStore.read(c,"healbe-energy");}catch(Exception e){return new JSONObject();}}
}
