package com.example.rokidgeminisecretary;
import org.json.*;
final class RecorderImportStatus {
 static String label(JSONObject state,JSONObject request,long fallback,long now){
  long at=request.optLong("createdAt",fallback);JSONArray rows=state.optJSONArray("transfers");boolean started=false;for(int i=0;rows!=null&&i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&"photo".equals(row.optString("kind"))&&row.optLong("startedAt")>=at-5000)started=true;}
  if(!started&&request.has("id")){if(!PhotoImportRequests.valid(request,now))return "取り込み要求の期限が切れました。原本はグラスに保持しています。接続を確認して再実行してください。";if(request.optLong("acceptedAt")>0)return now-request.optLong("acceptedAt")>30000?"グラスは受信済みですが、取り込みの開始を確認できません。原本を保持しています。接続を確認して再実行してください。":"グラスが取り込み要求を受信しました。KEEPの停止・写真保存の開始確認待ちです。";}
  return label(state,at,now);
 }
 static String label(JSONObject state,long requestAt,long now){
  JSONArray rows=state.optJSONArray("transfers");JSONObject photo=null;for(int i=0;rows!=null&&i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&"photo".equals(row.optString("kind")))photo=row;}
  if(requestAt>0&&(photo==null||photo.optLong("startedAt")<requestAt-5000))return now<requestAt||now-requestAt>30000?"取り込み要求の受信は未確認です。両端末のロキと接続を確認してください。保存完了ではありません。":"グラスへ取り込み要求を送信済み。受信・保存の確認待ちです。";
  if(photo==null)return "KEEP写真はグラス内に保存されます。スマホで見るには「グラスから取り込み」を選んでください。";
  return (state.optBoolean("connected")&&now>=photo.optLong("updatedAt")&&now-photo.optLong("updatedAt")<=30000?"":"前回受信した状態（現在の進行は未確認）：\n")+TransferProgressPolicy.label(photo);
 }
 private RecorderImportStatus(){}
}
