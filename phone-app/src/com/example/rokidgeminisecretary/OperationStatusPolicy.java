package com.example.rokidgeminisecretary;
import org.json.*;
/** Display projection only: never grants execution or rewrites authoritative receipts. */
final class OperationStatusPolicy {
 static JSONArray conversationRows(JSONArray records,int limit)throws Exception{
  // Unresolved work must not disappear behind newer completed requests.
  JSONArray selected=new JSONArray();int bound=Math.max(0,Math.min(100,limit));
  for(int pass=0;pass<2;pass++)for(int i=records.length()-1;i>=0&&selected.length()<bound;i--){
   JSONObject row=records.optJSONObject(i);if(row==null)continue;
   boolean resolved=row.optBoolean("tracked")&&("ANSWERED".equals(row.optString("requestState"))||"FAILED".equals(row.optString("requestState"))||"CANCELLED".equals(row.optString("requestState")));
   if(resolved==(pass==1))selected.put(new JSONObject(row.toString()));
  }return selected;
 }
 static JSONObject card(String type,String id,String title,String raw,long at,long now)throws Exception{
  if(type==null)type="";if(raw==null)raw="";if(title==null)title="";if(id==null)id="";
  String phase=raw.equals("QUEUED")||raw.equals("CREATED")?"queued":raw.equals("RECEIVED")?"received":raw.equals("RUNNING")?"running":raw.equals("AWAITING_CONFIRMATION")?"confirmation":raw.equals("DEFERRED")?"deferred":raw.equals("COMPLETED")||raw.equals("ANSWERED")?"completed":raw.equals("FAILED")?"failed":raw.equals("CANCELLED")?"cancelled":"needs_check";
  long age=at>0&&now>=at?now-at:-1;if(type.equals("conversation")&&(phase.equals("running")||phase.equals("received"))&&(age<0||age>300000))phase="needs_check";
  boolean staleActive=(phase.equals("running")||phase.equals("received")||phase.equals("queued"))&&(age<0||age>300000);
  String detail=phase.equals("needs_check")?"結果を確認できていません。自動再送はしていません。":phase.equals("queued")&&staleActive?"送信待ちが続いています。通信復帰だけでは実行の成否を判断できません。":phase.equals("running")&&staleActive?"最後の受付状態です。録音など継続中の処理もあるため、失敗とは判定していません。":"";
  String label=phase.equals("queued")?"送信待ち":phase.equals("received")?"受付済み":phase.equals("running")?"処理中":phase.equals("confirmation")?"内容の確認待ち":phase.equals("deferred")?"保留中":phase.equals("completed")?"完了を確認":phase.equals("failed")?"完了できませんでした":phase.equals("cancelled")?"取り消し済み":"結果未確認";
  return new JSONObject().put("type",type).put("id",id).put("title",title.substring(0,Math.min(80,title.length()))).put("phase",phase).put("label",label).put("detail",detail).put("stale",staleActive||phase.equals("needs_check")).put("at",at).put("ageMs",age).put("terminal",phase.equals("completed")||phase.equals("failed")||phase.equals("cancelled"));
 }
 private OperationStatusPolicy(){}
}