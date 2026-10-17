package com.example.rokidgeminisecretary;
import java.util.ArrayList;
/** Segments are distinct confirmed speech, while partials may be revised. Never overwrite older segments. */
final class SpeechTranscriptAccumulator {
 private final ArrayList<String> segments=new ArrayList<>(); private String partial="",full="";
 synchronized void segment(String text){if(text!=null&&!text.trim().isEmpty()){segments.add(text.trim());partial="";}}
 synchronized void partial(String text){partial=text==null?"":text.trim();}
 synchronized void full(String text){full=text==null?"":text.trim();partial="";}
 private String joined(){return String.join(" ",segments);}
 private static String norm(String s){return s.replaceAll("[\\s　、。，,.]","");}
 synchronized String confirmed(){String joined=joined();if(joined.isEmpty())return full;if(!full.isEmpty()&&norm(full).length()>=norm(joined).length())return full;return joined;}
 synchronized String preview(){String text=confirmed();if(partial.isEmpty())return text;if(text.isEmpty())return partial;if(norm(partial).startsWith(norm(text)))return partial;return text+" "+partial;}
 synchronized int count(){return segments.size();}
 static long timeoutMs(int bytes,int rate){return Math.min(90000L,Math.max(20000L,(long)bytes*1000/Math.max(1,rate*2)+20000L));}
}
