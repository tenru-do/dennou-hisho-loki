package com.example.rokidgeminisecretary;
import org.json.*;
/** Read-only status projection; calendar operations and persisted states are untouched. */
final class SecretaryInboxPresentation {
 static JSONObject decorate(JSONObject original,long now)throws Exception{
  JSONObject card=new JSONObject(original.toString());String state=card.optString("state"),stage;
  if("pending".equals(state)){boolean review=card.has("review")&&!card.optString("reviewToken").isEmpty();long at=card.optLong("reviewAt");stage=review?(at>0&&now>=at&&now-at<=600000?"review_wait":"review_expired"):"pending";}
  else stage="completed".equals(state)?"completed":"cancelled".equals(state)?"cancelled":"needs_check";
  return card.put("displayStage",stage).put("stateLabel",label(stage));
 }
 static String label(String stage){switch(stage){case "pending":return "保留";case "review_wait":return "登録確認待ち";case "review_expired":return "登録確認の期限切れ";case "completed":return "登録完了";case "cancelled":return "取消";default:return "登録結果要確認";}}
 static boolean active(JSONObject card){String state=card.optString("state");return !"completed".equals(state)&&!"cancelled".equals(state);}
 private SecretaryInboxPresentation(){}
}
