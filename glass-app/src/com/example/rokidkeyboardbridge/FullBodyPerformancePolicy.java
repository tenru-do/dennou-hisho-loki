package com.example.rokidkeyboardbridge;
import java.util.Locale;
/** Explicit requests only; topic discussion does not start a demonstration. */
final class FullBodyPerformancePolicy {
 static final int FIRST=252;
 static final long[] DURATION={500,650,180,240,400,250,350,500,650,300,650,650};
 static final long TOTAL=5320;
 static int[] frames(){return new int[]{252,253,254,255,256,257,258,259,260,261,262,263};}
 static int frame(long elapsed){long t=Math.max(0,elapsed);for(int i=0;i<DURATION.length;i++){if(t<DURATION[i])return i;t-=DURATION[i];}return 11;}
 static boolean requests(String text){
  String v=text==null?"":text.replaceAll("「[^」]*」|『[^』]*』", "").toLowerCase(Locale.ROOT).replace("・", "").replace(" ","");
  if(has(v,"見せない","見せなく","やらない","やらなく","しない","しなく","やめ","中止","やってほしくない","don’t","don't","stop","意味","って言","という依頼"))return false;
  return has(v,"ジークンドー","ジートクンドー","截拳道","jeetkunedo","jkd","ヌンチャク","ハイキック","ブルースリー")&&has(v,"見せて","やって","実演して","披露して","演じて","demonstrate","show");
 }
 private static boolean has(String text,String...words){for(String w:words)if(text.contains(w))return true;return false;}
}
