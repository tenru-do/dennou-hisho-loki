package com.example.rokidkeyboardbridge;
final class AmbientCommentPolicy {
 static boolean due(long now,long previous,int hour){return hour>=7&&hour<22&&(previous==0||now-previous>=3600000L);}
 static boolean accept(String text,String previous){
  if(text==null||text.trim().isEmpty()||text.trim().equalsIgnoreCase("SKIP"))return false;
  for(String word:new String[]{"寂し","さびし","淋し","構って","かまって","話しかけて","返事して","はぁ","マスター…"})if(text.contains(word))return false;
  return !text.replaceAll("[\\s、。！？!?]", "").equals(previous==null?"":previous.replaceAll("[\\s、。！？!?]", ""));
 }
}
