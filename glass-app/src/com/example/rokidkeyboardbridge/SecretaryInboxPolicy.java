package com.example.rokidkeyboardbridge;
import java.util.regex.*;
/** Explicit user intent only; reference text is never evaluated as a command. */
final class SecretaryInboxPolicy {
 static String compact(String s){return s==null?"":s.replaceAll("\\s","");}
 static boolean defer(String s){
  String t=compact(s);
  if(t.matches(".*(?:保留しない|保留にしない|保留しなく|保存しない|覚えない|とは言っていない).*"))return false;
  return t.matches(".*(?:保留にして|保留して|受信箱に(?:入れて|保存)|あとで(?:調べる|調べて|確認する|確認して|登録する|登録して)|後で(?:調べる|調べて|確認する|確認して|登録する|登録して)|聞いてから登録|確認してから登録|相談してから登録).*");
 }
 static boolean list(String s){return compact(s).matches("(?:秘書の)?(?:受信箱|保留案件|保留中の案件|保留した案件)(?:を)?(?:見せて|開いて|一覧|確認|教えて)?[。？?]?");}
 static boolean resume(String s){
  String t=compact(s);
  if(defer(t)||t.matches(".*(?:登録しない|登録はしない|登録しなく|登録しないで|やめて|キャンセル).*"))return false;
  return t.matches(".*(?:この前|先日|保留|受信箱|前のイベント|あのイベント).*")&&t.matches(".*(?:登録して|登録お願い|登録しといて|続けて|再開して).*");
 }
 static boolean supplement(String s){
  String t=compact(s);
  return t.length()<=100&&(t.matches("(?:[「『]?[^「」『』]{1,40}[」』]?(?:カレンダー)?(?:で|へ|に|だよ|です)|(?:登録先は)?[「『][^「」『』]{1,40}[」』](?:カレンダー)?(?:で|へ|に|だよ|です|に登録して)?|(?:日程|開始|終了|日時|場所|登録先|カレンダー)(?:は|を|:|：).*|(?:20[0-9]{2}年)?[0-9０-９]+月.*|それで|はい|お願いします|確定して登録|登録をキャンセル)"));
 }
 static String id(String s){Matcher m=Pattern.compile("案件ID:([a-f0-9-]{36})(?:\\s|$)").matcher(s==null?"":s);return m.find()?m.group(1):"";}
 static String title(String s){String t=s==null?"":s.trim();return t.length()>160?t.substring(0,160):t;}
}
