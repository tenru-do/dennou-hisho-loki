package com.example.rokidgeminisecretary;
import org.json.*;
/** Completion receipts and locally stored answer text are distinct facts. */
final class QuestionDelivery {
 static String describe(JSONObject row,boolean answerSaved){String state=row.optString("requestState"),reason=row.optString("receiptReason");if(answerSaved)return "回答受信済み・スマホに本文保存済み。再送は不要です";if(state.equals("ANSWERED"))return "メガネで回答完了・スマホの本文受信は未確認。同期を待ち、再送前にメガネの回答を確認してください";if(state.equals("CREATED")||state.equals("QUEUED"))return "メガネの受理未確認。送信待ちの内容を確認してください（到達していないと断定はできません）";if(state.equals("FAILED")){if(reason.equals("NETWORK"))return "AI通信で失敗した記録があります。部分的な結果や処理済み操作を確認してから再送を判断してください";return "回答未完了："+(reason.equals("AUTH")?"認証":reason.equals("RATE_LIMIT")?"利用制限":reason.equals("SAFETY_STOP")?"安全停止":reason.equals("MODEL_ERROR")?"AI処理エラー":"中断・要確認")+"。再送は自動で行いません";}if(state.equals("RECEIVED")||state.equals("RUNNING"))return "メガネ受理済み・"+(state.equals("RUNNING")?"AI処理中":"処理開始待ち")+"。新たな依頼としての再送は控えてください";return "結果は要確認です。送信控え・保存済み回答を確認してください";}
 static boolean saved(MainActivity c,String key){try{if(ImportedAnswers.received(c,key))return true;JSONArray rows=c.webHistory();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(key.equals(row.optString("requestId"))&&!row.optString("kind").startsWith("ユーザー"))return true;}}catch(Exception unavailable){}return false;}
 private QuestionDelivery(){}
}
