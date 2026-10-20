package com.example.rokidkeyboardbridge;
import org.json.*;
public final class ContentRestrictionPolicyTest {
 static int n;static void ok(boolean v){n++;if(!v)throw new AssertionError("check "+n);}
 static JSONObject candidate(String reason)throws Exception{return new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("finishReason",reason)));}
 public static void main(String[] args)throws Exception{
 ok(ContentRestrictionPolicy.blocked(candidate("STOP"))==null);ok(ContentRestrictionPolicy.blocked(candidate("MAX_TOKENS"))==null);ok(ContentRestrictionPolicy.blocked(new JSONObject())==null);
 for(String code:new String[]{"SAFETY","PROHIBITED_CONTENT","IMAGE_SAFETY","BLOCKLIST"})ok(ContentRestrictionPolicy.blocked(candidate(code)).category.equals("CONTENT"));
 ok(ContentRestrictionPolicy.blocked(candidate("SPII")).category.equals("PRIVACY"));ok(ContentRestrictionPolicy.blocked(candidate("RECITATION")).category.equals("COPYRIGHT"));
 JSONObject block=candidate("SAFETY");block.getJSONArray("candidates").getJSONObject(0).put("safetyRatings",new JSONArray().put(new JSONObject().put("category","HARM_CATEGORY_SEXUALLY_EXPLICIT").put("blocked",true)));ok(ContentRestrictionPolicy.blocked(block).category.equals("SEXUAL"));block.getJSONArray("candidates").getJSONObject(0).getJSONArray("safetyRatings").getJSONObject(0).put("blocked",false);ok(ContentRestrictionPolicy.blocked(block).category.equals("CONTENT"));
 ok(ContentRestrictionPolicy.blocked(new JSONObject().put("promptFeedback",new JSONObject().put("blockReason","PROHIBITED_CONTENT"))).category.equals("CONTENT"));
 for(String v:new String[]{"性的な描写には対応できません。別の内容ならお手伝いします。","申し訳ありません。性的な内容にはお応えできません。","マスター、申し訳ございません。この性的な依頼には対応できません。"})ok(ContentRestrictionPolicy.refusal(v).category.equals("SEXUAL"));
 ok(ContentRestrictionPolicy.refusal("この依頼にはお応えできません。").category.equals("UNKNOWN"));
 for(String v:new String[]{"「対応できません」という表現の意味は、依頼を受けられないという意味です。","通信に失敗したため対応できません。","性的な言葉は使わずに、ここを説明します。","この件は断定できませんが、確認する方法があります。","対応できませんという例文を説明します。"})ok(ContentRestrictionPolicy.refusal(v)==null);
 for(String c:new String[]{"SEXUAL","PRIVACY","COPYRIGHT","DANGEROUS","ABUSE","CONTENT","UNKNOWN"}){String m=ContentRestrictionPolicy.message("回答",c);ok(m.contains("自動再送はしていません"));ok(!m.contains("PRIVATE")&&!m.contains("http"));}
 ok(ContentRestrictionPolicy.message("回答","CONTENT").contains("具体的な判定項目は確認できません"));ok(ContentRestrictionPolicy.message("回答","UNKNOWN").contains("理由は示していません"));
 ok(ConversationFailurePolicy.code(new ContentRestrictionPolicy.Stop("SEXUAL"),0,"",false).equals("SAFETY_STOP"));ok(ConversationFailurePolicy.code(new ContentRestrictionPolicy.Stop("UNKNOWN"),0,"",false).equals("MODEL_DECLINED"));ok(ConversationFailurePolicy.detail(new ContentRestrictionPolicy.Stop("SEXUAL"),"SAFETY_STOP").contains("性的な表現"));
 System.out.println("PASS "+n+" content cause / unknown cause / quoted refusal / privacy checks");
 }
}
