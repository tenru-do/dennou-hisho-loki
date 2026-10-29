package com.example.rokidgeminisecretary;
import java.text.Normalizer;
import java.util.Locale;
/** RSS descriptions often repeat the headline with only a publisher suffix. */
final class NewsSpeechText {
 static String key(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{Z}\\s]+","");}
 static String headline(String s){return (s==null?"":s).replaceFirst("\\s+[-–—|]\\s+[^\\r\\n]+$","").trim();}
 static boolean sameHeadline(String a,String b){String x=key(headline(a)),y=key(headline(b));return !x.isEmpty()&&x.equals(y);}
 static boolean update(String previous,String current){String a=key(previous),b=key(current);if(a.equals(b))return false;String left=a.replaceAll("[^0-9]",""),right=b.replaceAll("[^0-9]","");if(!left.equals(right)&&!right.isEmpty())return true;for(String word:new String[]{"再開","中止","終了","開始","決定","判明","拡大","縮小","更新"})if(b.contains(word)&&!a.contains(word))return true;return false;}
 static String detail(String title,String description,String publisher){
  String d=description==null?"":description.trim(),h=headline(title),k=key(d),t=key(h);
  if(k.isEmpty()||k.equals(t)||sameHeadline(title,d))return "";
  String p=key(publisher);
  if(!p.isEmpty()&&(k.equals(t+p)||k.equals(t+"全文を読む"+p)))return "";
  // Remove only a literal leading headline; retain genuinely new details.
  if(!h.isEmpty()&&d.startsWith(h)){
   String tail=d.substring(h.length()).replaceFirst("^[\\s。．、:：\\-–—|]+","").trim();
   if(key(tail).equals(p))return "";
   return tail;
  }
  return d;
 }
 private NewsSpeechText(){}
}
