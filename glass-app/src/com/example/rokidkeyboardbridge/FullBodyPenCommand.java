package com.example.rokidkeyboardbridge;
import java.util.Locale;
final class FullBodyPenCommand {
 static final int[] SEQUENCE={2,3,4,5};
 static final long[] DURATION={1000,1200,1400,1800};
 static final long TOTAL=5400;
 static int frame(long elapsed){long t=Math.max(0,elapsed);for(int i=0;i<DURATION.length;i++){if(t<DURATION[i])return SEQUENCE[i];t-=DURATION[i];}return 5;}
 static boolean requests(String text){
  String v=text==null?"":text.replaceAll("「[^」]*」|『[^』]*』", "").toLowerCase(Locale.ROOT).replace(" ","");
  if(v.contains("拾わない")||v.contains("拾わなく")||v.contains("ひろわない")||v.contains("拾ってほしくない")||v.contains("拾って欲しくない")||v.contains("意味")||v.contains("don’t")||v.contains("don't"))return false;
  if(v.contains("落ちたら")||v.contains("落としたら")||v.contains("もし")||v.contains("落ちていない")||v.contains("落ちてない")||v.contains("落とさない"))return false;
  if(v.contains("ペンが落ちた")||v.contains("ペンが落ちて")||v.contains("ペン落ちた")||v.contains("ペンを落とした")||v.contains("ペンを落としてしま")||v.contains("ペンを落としちゃ")||v.contains("ペンが床に"))return true;
  return v.contains("ペン")&&(v.contains("拾って")||v.contains("ひろって")||v.contains("拾い上げて"))||v.contains("pickupthepen");
 }
}
