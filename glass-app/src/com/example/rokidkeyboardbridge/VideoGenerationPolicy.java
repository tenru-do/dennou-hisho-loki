package com.example.rokidkeyboardbridge;
/** Paid generation requires an explicit creative request, never a capability question or video analysis. */
final class VideoGenerationPolicy {
 static boolean requests(String s){
  if(s==null||s.trim().isEmpty()||s.startsWith("__")||s.matches("(?s).*(作らないで|生成しないで|動画にしないで|動かさないで|生成でき|作れます|作れる|できますか|可能ですか|方法を|機能を|実装して|インストール|動画を削除|解析して|分析して|読み取って|何が映|何が写).*") )return false;
  return s.matches("(?s).*(?:動画|ビデオ|動画化).*(?:作って|作成して|生成して|生成しろ|にして|してほしい|して欲しい|してください|描いて).*" )||s.matches("(?s).*(?:この画像|その画像|この絵|さっきの画像|生成した画像).*(?:動かして|動かしてください|アニメにして|アニメーションにして).*" );
 }
 static boolean usesImage(String s){return s!=null&&s.matches("(?s).*(この画像|その画像|この絵|さっきの画像|生成した画像|この写真|その写真).*");}
 static String aspect(String s,boolean self){return s!=null&&s.matches("(?s).*(横長|横向き|16[:：]9).*" )?"16:9":self||s!=null&&s.matches("(?s).*(縦長|縦向き|9[:：]16).*" )?"9:16":"16:9";}
 private VideoGenerationPolicy(){}
}
