package com.example.rokidkeyboardbridge;
import java.time.*;
/** Tracks consecutive fresh observations, not inferred sedentary time or medical targets. */
final class ExerciseNudgePolicy {
 private long previousAt,previousCount=-1,unchangedSince;
 private String day="",source="";
 static boolean off(String s){return s!=null&&s.matches("(?s).*(運動|歩数).*(声かけ|声掛け|アドバイス|通知).*(止め|やめ|停止|オフ|不要).*" );}
 static boolean on(String s){return s!=null&&s.matches("(?s).*(運動|歩数).*(声かけ|声掛け|アドバイス|通知).*(再開|オン|有効).*" );}
 String observe(StepsExerciseAdvice.Sample sample,long now,ZoneId zone,String lastDay,boolean blocked){
  long count=StepsExerciseAdvice.steps(sample.compact),age=now-sample.at;
  LocalDate date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();String today=date.toString();
  boolean valid=count>=0&&sample.at>0&&age>=0&&age<=300000&&Instant.ofEpochMilli(sample.at).atZone(zone).toLocalDate().equals(date);
  if(!valid){reset();return "";}
  // Repeated polling of the same record does not prove an hour of inactivity.
  if(sample.at==previousAt)return "";
  if(!today.equals(day)||!source.equals(sample.source)||sample.at<previousAt||sample.at-previousAt>300000||count!=previousCount)unchangedSince=sample.at;
  previousAt=sample.at;previousCount=count;day=today;source=sample.source;
  int hour=Instant.ofEpochMilli(now).atZone(zone).getHour();
  if(blocked){unchangedSince=sample.at;return "";}
  if(today.equals(lastDay)||hour<9||hour>=20)return "";
  if(sample.at-unchangedSince>=3600000L)return "マスター、歩数の記録がしばらく増えていないようです。座っていたなら、気分転換に少し立って動きませんか。";
  if(hour>=17&&count<3000&&"Health Connect".equals(source))return "マスター、今日の連携歩数は今のところ"+count+"歩です。未同期の記録や自転車移動もあるので、これだけで運動不足とは限りません。体調と時間に余裕があれば、短い散歩はいかがでしょう。疲れていたら休みましょう。";
  return "";
 }
 private void reset(){previousAt=0;previousCount=-1;unchangedSince=0;day="";source="";}
}
