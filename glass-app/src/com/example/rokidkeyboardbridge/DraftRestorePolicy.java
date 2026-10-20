package com.example.rokidkeyboardbridge;
/** A confirmation applies only to the exact draft that was shown. */
final class DraftRestorePolicy {
 static String reason(String expected,String current,boolean attached,boolean busy){
  if(busy)return "添付や音声を処理中です。完了してから開き直してください。";
  if(attached)return "送信前の添付があります。添付を外してからメモを戻してください。";
  if(expected==null||!expected.equals(current))return "確認中に入力が変わりました。現在の入力は上書きしていません。メモを選び直してください。";
  return "";
 }
 private DraftRestorePolicy(){}
}
