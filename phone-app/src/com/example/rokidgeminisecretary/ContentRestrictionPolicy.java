package com.example.rokidgeminisecretary;
import org.json.*;
/** Fixed explanations only. Never repeats provider payloads or guesses from the user's prompt. */
final class ContentRestrictionPolicy {
 static final class Stop extends java.io.IOException {final String category;Stop(String c){super("CONTENT_RESTRICTION");category=c;}}
 static String category(String code){
  if(code==null)return "";
  switch(code){case "HARM_CATEGORY_SEXUALLY_EXPLICIT":return "SEXUAL";case "HARM_CATEGORY_DANGEROUS_CONTENT":return "DANGEROUS";case "HARM_CATEGORY_HARASSMENT":case "HARM_CATEGORY_HATE_SPEECH":return "ABUSE";case "SPII":return "PRIVACY";case "RECITATION":return "COPYRIGHT";case "SAFETY":case "PROHIBITED_CONTENT":case "IMAGE_SAFETY":case "IMAGE_PROHIBITED_CONTENT":case "BLOCKLIST":case "MODEL_ARMOR":return "CONTENT";default:return "";}
 }
 static String blockedRatings(JSONArray ratings){if(ratings!=null)for(int i=0;i<ratings.length();i++){JSONObject item=ratings.optJSONObject(i);if(item!=null&&item.optBoolean("blocked")){String c=category(item.optString("category"));if(!c.isEmpty())return c;}}return "";}
 static Stop blocked(JSONObject response){
  if(response==null)return null;JSONObject feedback=response.optJSONObject("promptFeedback");String c=feedback==null?"":blockedRatings(feedback.optJSONArray("safetyRatings"));
  if(c.isEmpty()&&feedback!=null)c=category(feedback.optString("blockReason"));
  JSONArray candidates=response.optJSONArray("candidates");if(candidates!=null)for(int i=0;i<candidates.length()&&c.isEmpty();i++){JSONObject candidate=candidates.optJSONObject(i);if(candidate==null)continue;c=blockedRatings(candidate.optJSONArray("safetyRatings"));if(c.isEmpty())c=category(candidate.optString("finishReason"));}
  return c.isEmpty()?null:new Stop(c);
 }
 static Stop refusal(String reply){
  String v=reply==null?"":reply.trim();if(v.length()>700||v.startsWith("「")||v.startsWith("『")||v.startsWith("\""))return null;
  v=v.replaceFirst("^(?:マスター[、,\\s]*)?(?:申し訳(?:ございません|ありません|ないですが)[、,。\\s]*)?", "");
  int end=v.indexOf('。');String first=end<0?v:v.substring(0,end+1);if(first.length()>300)return null;
  boolean declined=first.matches("(?s).*(?:お応え(?:することは|するのは)?できません|対応(?:することは|するのは)?できません|お手伝い(?:することは|するのは)?できません|お引き受けできません|お応えしかねます|お応えするのが難しい).*" );
  if(!declined||first.contains("「")||first.contains("『")||first.matches("(?s).*(?:通信|接続|APIキー|利用上限|権限|という表現|例文|という意味).*"))return null;
  String c=v.contains("性的")||v.contains("露骨な性")?"SEXUAL":v.contains("個人情報")||v.contains("プライバシー")?"PRIVACY":v.contains("著作権")?"COPYRIGHT":v.contains("暴力")||v.contains("危害")?"DANGEROUS":v.contains("差別")||v.contains("嫌がらせ")?"ABUSE":"UNKNOWN";
  return new Stop(c);
 }
 static void requireReply(String reply)throws Stop{Stop s=refusal(reply);if(s!=null)throw s;}
 static String reason(Throwable error){for(int i=0;error!=null&&i<8;i++,error=error.getCause()){if(error instanceof Stop)return ((Stop)error).category;String m=error.getMessage();if(m!=null){if(m.contains("SPII"))return "PRIVACY";if(m.contains("RECITATION"))return "COPYRIGHT";}}return "CONTENT";}
 static String message(String operation,String c){
  String reason;switch(c){case "SEXUAL":reason="AI側が性的な表現に関する制限に該当すると判定しました。";break;case "PRIVACY":reason="個人の機微情報やプライバシーに関する制限で停止しました。";break;case "COPYRIGHT":reason="著作権保護のための引用・再現に関する制限で停止しました。";break;case "DANGEROUS":reason="暴力・危険な行為などに関する内容制限で停止しました。";break;case "ABUSE":reason="差別・嫌がらせなどに関する内容制限で停止しました。";break;case "UNKNOWN":reason="AIは対応できないと回答しましたが、具体的な理由は示していません。";break;default:reason="AI側の内容制限で停止しました。具体的な判定項目は確認できません。";}
  return operation+"を完了できません。"+reason+"自動再送はしていません。";
 }
 private ContentRestrictionPolicy(){}
}
