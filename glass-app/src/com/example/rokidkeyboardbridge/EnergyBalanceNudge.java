package com.example.rokidkeyboardbridge;
import java.time.*;
final class EnergyBalanceNudge {
 static String advice(int balance,long observedAt,String day,String source,StepsExerciseAdvice.Sample steps,long now,ZoneId zone,String lastDay,boolean blocked){
  ZonedDateTime clock=Instant.ofEpochMilli(now).atZone(zone);String today=clock.toLocalDate().toString();
  if(blocked||today.equals(lastDay)||clock.getHour()<15||clock.getHour()>=20)return "";
  if(balance<300||balance>15000||observedAt<=0||now<observedAt||now-observedAt>900000||!today.equals(day))return "";
  if(!today.equals(Instant.ofEpochMilli(observedAt).atZone(zone).toLocalDate().toString())||!source.matches("HEALBE_SCREEN|HEALBE_NOTIFICATION|HEALBE_SDK"))return "";
  long count=StepsExerciseAdvice.steps(steps.compact);
  if(count<0||count>=5000||!"Health Connect".equals(steps.source)||steps.at<=0||now<steps.at||now-steps.at>300000)return "";
  if(!today.equals(Instant.ofEpochMilli(steps.at).atZone(zone).toLocalDate().toString()))return "";
  return "マスター、GoBeの暫定のエネルギー収支がプラスで、連携歩数も少なめです。体調がよければ、気分転換に短い散歩はいかがでしょう。数値は後から変わるので、無理に相殺しなくて大丈夫です。";
 }
}
