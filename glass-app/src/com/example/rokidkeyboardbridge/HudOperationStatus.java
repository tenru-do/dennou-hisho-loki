package com.example.rokidkeyboardbridge;
/** Fixed-size HUD status; never shares the comment surface. */
final class HudOperationStatus {
 static String compact(String text){if(text==null)return "";String s=text.replaceAll("[\\r\\n\\t]+"," ").trim();return s.length()>64?s.substring(0,63)+"…":s;}
 static String compact(String text,long age,boolean busy){String s=compact(text);return busy&&!s.isEmpty()?s+"（"+Math.min(999,Math.max(0,age)/1000)+"秒）":s;}
 private HudOperationStatus(){}
}
