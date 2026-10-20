package com.example.rokidgeminisecretary;
import org.json.*;
/** Shared fixed vocabulary. No response text or user content in progress records. */
final class ConversationDeliveryState {
 static JSONObject safe(JSONObject v)throws Exception{
  String phase=v.optString("phase"),stage=v.optString("stage"),id=v.optString("id"),reason=v.optString("reason");
  if(!phase.matches("IDLE|RUNNING|ANSWERED|FAILED|UNCONFIRMED"))phase="UNCONFIRMED";
  if(!stage.matches("PREPARE|ANSWER|REVIEW|REPAIR|DONE"))stage="";
  if(!id.matches("[a-f0-9]{64}"))id="";
  if(!reason.matches("|REFERENCE_CHANGED|SAFETY_STOP|MODEL_DECLINED|TIMEOUT|CONNECTION|INTERRUPTED|RATE_LIMIT|AUTHORIZATION|BILLING|MODEL_UNAVAILABLE|SERVICE_UNAVAILABLE|API_REJECTED|OUTPUT_LIMIT|NO_ANSWER|PROCESSING|INCOMPLETE_RESPONSE|RESTART"))reason="PROCESSING";
  return new JSONObject().put("phase",phase).put("stage",stage).put("id",id).put("reason",reason).put("at",Math.max(0,v.optLong("at")));
 }
 static String label(JSONObject raw,boolean fresh)throws Exception{
  JSONObject v=safe(raw);String phase=v.getString("phase"),stage=v.getString("stage");
  String label=phase.equals("ANSWERED")?"回答完了":phase.equals("FAILED")?"回答未完了":phase.equals("IDLE")?"待機中":phase.equals("UNCONFIRMED")?"結果未確認":stage.equals("PREPARE")?"会話を整理中":stage.equals("REVIEW")?"回答を確認中":stage.equals("REPAIR")?"回答を調整中":"回答待ち";
  if(!fresh&&!phase.equals("IDLE"))label="最後の状態："+label+"（現在の結果は未確認）";
  if(!v.getString("reason").isEmpty())label+=" / "+v.getString("reason");
  return label;
 }
 private ConversationDeliveryState(){}
}
