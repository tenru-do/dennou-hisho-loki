package com.example.rokidkeyboardbridge;
import java.time.*;
final class HealthNudgeCadence {
 static final long INTERVAL=3600000L;
 static boolean restRequested(String text){return text!=null&&!text.matches("(?s).*(休みたくない|休まなくていい).*" )&&text.matches("(?s).*(休みたい|休ませて|今日は休む|運動.*(?:したくない|勧めないで)).*" );}
 static boolean allowed(long now,long lastAt,String signature,String lastSignature){
  return now>0&&!signature.isEmpty()&&(lastAt<=0||now>=lastAt&&now-lastAt>=INTERVAL)&&!signature.equals(lastSignature);
 }
 static String signature(String reason,long steps,int energy,long now,ZoneId zone){
  if(!reason.matches("move_break|short_walk|energy_walk")||steps<0)return "";
  return Instant.ofEpochMilli(now).atZone(zone).toLocalDate()+"|"+reason+"|"+(steps/1000)+(reason.equals("energy_walk")?"|"+(energy/500):"");
 }
}
