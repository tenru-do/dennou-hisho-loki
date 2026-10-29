package com.example.rokidkeyboardbridge;
import java.time.*;import org.json.*;
/** Compact HUD values; freshness rules are display-only and never create measurements. */
final class HealthMetricsLine {
 static String format(StepsExerciseAdvice.Sample steps,JSONObject energy,long now,ZoneId zone){
  long count=StepsExerciseAdvice.steps(steps.compact);
  String walking="歩数 "+(count<0?"--":Long.toString(count));
  if(count>=0&&!HealthConversationContext.current(steps.at,now,300000,zone))walking+="(旧)";
  if(energy==null)return walking+"  収支 --";
  int balance=energy.optInt("balanceKcal",Integer.MIN_VALUE);long at=EnergySampleTime.at(energy);String source=energy.optString("source");
  if(balance< -15000||balance>15000||at<=0||!source.matches("HEALBE_SCREEN|HEALBE_NOTIFICATION|HEALBE_SDK"))return walking+"  収支 --";
  String today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString();
  if(now<at||!today.equals(energy.optString("date"))||!today.equals(Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toString()))return walking+"  収支 --(旧)";
  return walking+"  収支"+(balance>0?"+":"")+balance+"kcal"+(now-at>900000?"(旧)":"※");
 }
}
