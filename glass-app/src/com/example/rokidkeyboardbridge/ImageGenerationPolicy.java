package com.example.rokidkeyboardbridge;
/** Only a generation/edit request starts the paid image API; questions about the feature do not. */
final class ImageGenerationPolicy {
 static boolean edit(String s){return s!=null && s.matches("(?s).*(背景|この画像|その画像|この絵|さっきの画像|直前の画像|生成した画像|もっとリアル|もう少しリアル|色を|明るく|暗く|イラストにして|写真風にして|描き直して).*");}
 static boolean requests(String s,boolean recent){
  if(s==null||s.trim().isEmpty()||s.startsWith("__")||s.matches("(?s).*(生成でき|生成できます|生成可能|生成する方法|生成することは|描ける|描けます|機能を|実装して|インストール|描かないで|生成しないで|作らないで|画像を削除).*") )return false;
  boolean explicit=s.matches("(?s).*(描いて|描け|描き直して|画像生成して|画像を生成|画像.*作って|イラスト.*作って|絵を作って|イラストにして|写真風にして).*");
  if(explicit)return true;
  if(s.matches("(?s).*(説明して|教えて|読み取|解析して|何が写|何が描).*") )return false;
  return (recent && edit(s) && (s.matches("(?s).*(して|変えて|変更|もっと|もう少し|にする|にして).*") || s.matches("(?s).*背景.*(?:に|く)$")));
 }
 private ImageGenerationPolicy(){}
}
