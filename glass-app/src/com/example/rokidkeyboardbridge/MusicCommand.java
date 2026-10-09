package com.example.rokidkeyboardbridge;
/** Explicit music operations only: never intercept bare stop/back or discussion. */
final class MusicCommand {
 final String action,source; final long delta;
 MusicCommand(String a,String s,long d){action=a;source=s;delta=d;}
 static MusicCommand parse(String text){
  String v=text==null?"":java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFKC).replaceAll("[\\s　]","");
  if(v.contains("「")||v.contains("『")||has(v,"しない","しないで","止めない","停止しない","とは","方法","できる","機能","もし","したら","教えて"))return null;
  if(!has(v,"音楽","曲","ミュージック","Bluetooth","ブルートゥース","再生中"))return null;
  String source=has(v,"タブレット")?"tablet":has(v,"スマホ","スマートフォン")?"phone":"auto";
  if(has(v,"止め","停止","一時停止","ストップ"))return new MusicCommand("pause",source,0);
  if(has(v,"再開","再生して","再生しよう","かけて","流して"))return new MusicCommand("play",source,0);
  return null;
 }
 static boolean has(String v,String...words){for(String w:words)if(v.contains(w))return true;return false;}
}
