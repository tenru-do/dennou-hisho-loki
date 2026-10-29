package com.example.rokidgeminisecretary;
import java.util.regex.*;
/** Only explicitly labelled energy BALANCE, never intake or active calories. */
final class HealbeEnergyText {
 static Integer balance(String raw){
  if(raw==null||raw.length()>16000)return null;
  String s=raw.replace('−','-').replace('＋','+').replace('，',',');
  Matcher m=Pattern.compile("(?i)(?:エネルギー\\s*バランス|energy\\s*balance)\\s*[:：]?\\s*(?:[0-9,]+\\s*(?:歩|steps)[^\\r\\n]*[\\r\\n]+\\s*)?([+-]?\\s*(?:[0-9]{1,2}(?:,[0-9]{3})+|[0-9]{1,5}))\\s*kcal\\b").matcher(s);
  if(!m.find())return null;
  int n=Integer.parseInt(m.group(1).replaceAll("[\\s,]", ""));
  if(Math.abs(n)>15000||m.find())return null;
  return n;
 }
 static boolean todayScreen(String text){
  if(text==null||text.matches("(?is).*(昨日|一昨日|yesterday|履歴|history).*"))return false;
  return Pattern.compile("(?im)^\\s*(?:今日|本日|今日の概要|today|today's summary)(?:\\s*[,、，][^\\r\\n]*)?\\s*$").matcher(text).find();
 }
}
