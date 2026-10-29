package com.example.rokidkeyboardbridge;
/** Personal form of address is ordinary conversation, never an inbox case revision. */
final class UserAddressIntent{
 static boolean concerns(String text){if(text==null||text.length()>3000)return false;String t=text.replace(" ","");if(t.matches("(?s).*(案件|受信箱|担当者|予定の|ファイル名|タイトル|登場人物|彼の|彼女の).*"))return false;return t.matches("(?s).*(私の呼び方|僕の呼び方|俺の呼び方|私の呼び名|私を.{0,30}と呼|マスター.{0,4}(と呼んで|って呼んで|と呼ぶ|にしよう|にして|でいい)).*");}
 static boolean master(String text){return concerns(text)&&text.contains("マスター")&&!text.matches("(?s).*(呼ばない|呼ぶな|やめ|ではなく|じゃなく).{0,10}$");}
 private UserAddressIntent(){}
}
