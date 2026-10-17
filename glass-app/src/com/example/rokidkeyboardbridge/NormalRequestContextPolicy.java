package com.example.rokidkeyboardbridge;
/** Separate stored roleplay directives from ordinary assistant requests; never changes stored settings. */
final class NormalRequestContextPolicy {
 static String custom(String raw){
  if(raw==null)return "";
  StringBuilder kept=new StringBuilder();
  for(String part:raw.replace("\r", "").split("(?<=[。！？!?\n])")){
   String s=part.trim();if(s.isEmpty())continue;
   if(s.matches("(?s).*(?:セクハラ|身体を許|体を許|あえぎ声|喘ぎ声|性には|性的な演技|ちんこ|ち○こ|チンチン|暗い穴のなか).*"))continue;
   if(kept.length()>0)kept.append('\n');kept.append(s);
  }return kept.toString();
 }
 static boolean safetyStop(String reason){
  if(reason==null)return false;
  return reason.matches("(?i).*(?:PROHIBITED_CONTENT|SAFETY|BLOCKLIST|IMAGE_SAFETY|SPII|RECITATION).*");
 }
}
