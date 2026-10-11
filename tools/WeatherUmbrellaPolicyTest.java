package com.example.rokidkeyboardbridge;
public final class WeatherUmbrellaPolicyTest {
 public static void main(String[] args){
  for(String s:new String[]{"クッションのやわらかさを教えて","説明の確かさは？","温かさを感じる"})if(WeatherUmbrellaPolicy.kanaUmbrella(s))throw new AssertionError(s);
  for(String s:new String[]{"かさ？","かさは必要？","今日はかさを持って行く？","カサいる？"})if(!WeatherUmbrellaPolicy.kanaUmbrella(s))throw new AssertionError(s);
  System.out.println("PASS kana umbrella intent excludes unrelated word suffixes");
 }
}
