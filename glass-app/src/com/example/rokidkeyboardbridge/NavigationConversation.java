package com.example.rokidkeyboardbridge;
/** Local navigation intents. Unknown language remains ordinary conversation. */
public final class NavigationConversation {
 public static String intent(String input,boolean active){
  if(!active||input==null)return "";String s=input.trim().replaceAll("[\\s　。？！?!]","");
  if(s.matches("(あと|目的地まであと|到着まであと)(どれくらい|どのくらい|何分|何キロ)(かかる|ですか)?"))return "remaining";
  if(s.matches("(次|つぎ)(は)?(どっち|どちら|どこ|どう行く)(ですか)?")||s.matches("(もう一回|もう一度|案内をもう一度|次の案内)(教えて|お願い)?"))return "next";
  if(s.matches("(そっちじゃない|そちらじゃない|そっちではない|違う道|道が違う)"))return "clarify";
  if(s.matches("(目的地|行き先)(が違う|を変えたい|を変更したい)"))return "destination";
  if(s.matches("(道順|経路|ルート)(が違う|のこと|を確認したい)"))return "route";
  if(s.matches("(その前に|途中で|先に).{1,80}")&&!s.matches(".*(説明|教えて|聞きたい|質問|計算|翻訳|読んで|確認|検索の|操作|設定|話して).*"))return "stopover";
  if(s.matches("やっぱり.{1,80}(へ|に)(行きたい|行って|変更して)"))return "change";
  return "";
 }
 public static String clarified(String text,long until,long now){if(text==null||until<=0||now>until||now<until-60000)return text;String s=text.replaceAll("[\\s　。？！?!]","");if(s.matches("道順|経路|ルート"))return "道順のこと";if(s.matches("目的地|行き先"))return "目的地が違う";return text;}
 public static String target(String input,String intent){String s=input==null?"":input.trim().replaceFirst("[。？！?!]+$","");if(intent.equals("stopover"))return s.replaceFirst("^(その前に|途中で|先に)","").replaceFirst("(に寄りたい|へ寄りたい|に寄って|へ寄って|に立ち寄りたい|に立ち寄って)$","").trim();if(intent.equals("change"))return s.replaceFirst("^やっぱり","").replaceFirst("(へ|に)(行きたい|行って|変更して)$","").trim();return "";}
 public static String remaining(boolean usable,long now,long at,String duration,String distance){if(!usable||at<=0||now<at||now-at>30000)return "位置を確認中です。残り時間と距離は更新をお待ちください。";String d=duration==null?"":duration.trim(),m=distance==null?"":distance.trim();return d.isEmpty()&&m.isEmpty()?"残り時間と距離を取得中です。":"目的地まで、あと"+d+(!d.isEmpty()&&!m.isEmpty()?"、":"")+m+"です。";}
 public static String concise(String text){if(text==null)return "";String s=text.replaceAll("<[^>]*>","").replaceAll("[\\r\\n]+","、").trim();if(s.length()<=110)return s;int end=s.lastIndexOf("。",109);return end>=12?s.substring(0,end+1):s.substring(0,110)+"。";}
 private NavigationConversation(){}
}
