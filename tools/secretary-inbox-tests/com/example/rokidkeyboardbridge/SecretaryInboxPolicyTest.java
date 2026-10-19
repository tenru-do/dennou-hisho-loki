package com.example.rokidkeyboardbridge;public class SecretaryInboxPolicyTest{static int n;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;}public static void main(String[]a){
 for(String s:new String[]{"このイベント、家族に聞いてから登録したい","確認してから登録する","後で登録して","あとで調べて","受信箱に保存","これを保留にして"})ok(SecretaryInboxPolicy.defer(s),"defer "+s);
 for(String s:new String[]{"家族カレンダーに登録して","登録してある","保留しないで","これは保存しない","この前のイベントを登録して"})ok(!SecretaryInboxPolicy.defer(s),"not defer "+s);
 for(String s:new String[]{"この前のイベント、登録して","保留案件を続けて","受信箱の案件ID:00000000-0000-0000-0000-000000000000 を登録して"})ok(SecretaryInboxPolicy.resume(s),"resume "+s);
 for(String s:new String[]{"後で登録して","保留したイベントは登録しないで","家族に聞いてから登録したい","こんにちは","会話の続きを話して"})ok(!SecretaryInboxPolicy.resume(s),"not resume "+s);
 ok(SecretaryInboxPolicy.list("秘書の受信箱を開いて"),"list");ok(!SecretaryInboxPolicy.list("受信箱を削除して"),"not delete-as-list");
 ok(SecretaryInboxPolicy.id("受信箱の案件ID:00000000-0000-0000-0000-000000000000 を登録して").length()==36,"id");
 ok(!SecretaryInboxPolicy.supplement("別のイベントを家族カレンダーに登録して"),"new target does not retain");
 for(String s:new String[]{"「家族」カレンダーだよ","日程は10月24日","家族カレンダーで"})ok(SecretaryInboxPolicy.supplement(s),"supplement "+s);
 System.out.println("PASS secretary inbox intent "+n+" checks");
}}