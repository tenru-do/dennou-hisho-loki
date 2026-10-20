package com.example.rokidkeyboardbridge;
import org.json.*;
/** A receipt is completion evidence; transport activity alone is not. */
final class TransferProgressPolicy {
 static JSONObject safe(JSONObject input)throws Exception{
  String kind=input.optString("kind"),phase=input.optString("phase"),id=input.optString("batchId");if(!kind.matches("photo|meeting")||!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("invalid transfer identity");if(!phase.matches("RUNNING|COMPLETED|PARTIAL|FAILED|INTERRUPTED"))phase="UNKNOWN";
  long expected=Math.max(0,Math.min(100000,input.optLong("expected"))),confirmed=Math.max(0,Math.min(expected,input.optLong("confirmed"))),retained=Math.max(0,Math.min(expected-confirmed,input.optLong("retained"))),failed=Math.max(0,Math.min(expected-confirmed-retained,input.optLong("failed"))),deferred=Math.max(0,Math.min(expected-confirmed-retained-failed,input.optLong("deferred")));long units=Math.max(0,Math.min(720000,input.optLong("unitsExpected"))),done=Math.max(0,Math.min(units,input.optLong("unitsConfirmed")));
  if(phase.equals("COMPLETED")&&(failed>0||deferred>0||confirmed+retained!=expected||done!=units))phase="PARTIAL";
  return new JSONObject().put("kind",kind).put("batchId",id).put("phase",phase).put("expected",expected).put("confirmed",confirmed).put("retained",retained).put("failed",failed).put("deferred",deferred).put("unitsExpected",units).put("unitsConfirmed",done).put("startedAt",Math.max(0,input.optLong("startedAt"))).put("updatedAt",Math.max(0,input.optLong("updatedAt")));
 }
 static String label(JSONObject j){String phase=j.optString("phase"),kind=j.optString("kind").equals("photo")?"写真":"録音";String status=phase.equals("COMPLETED")?"転送処理完了":phase.equals("RUNNING")?"転送中":phase.equals("PARTIAL")?"一部を確認できていません":phase.equals("FAILED")?"転送を完了できませんでした":phase.equals("INTERRUPTED")?"前回中断・転送結果を確認してください":"結果未確認";
  return kind+"："+status+" ／ スマホ保存確認 "+j.optLong("confirmed")+" / "+j.optLong("expected")+"件・保存未確認 "+Math.max(0,j.optLong("expected")-j.optLong("confirmed")-j.optLong("retained"))+"件（失敗・別処理待ちを含む）"+(j.optLong("retained")>0?"・過去の削除を尊重してグラス保持 "+j.optLong("retained")+"件":"")+(j.optLong("deferred")>0?"・別の取り込み待ち "+j.optLong("deferred")+"件":"")+(j.optLong("failed")>0?"・失敗 "+j.optLong("failed")+"件":"")+(j.optLong("unitsExpected")>0?" ／ 保存区間確認 "+j.optLong("unitsConfirmed")+" / "+j.optLong("unitsExpected"):"");
 }
 private TransferProgressPolicy(){}
}
