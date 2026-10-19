package com.example.rokidkeyboardbridge;
import org.json.*;
public class ConversationContextReviewTest {
 static int count;static void ok(boolean value,String label){if(!value)throw new AssertionError(label);count++;}
 static JSONObject flagged(String issue,String evidence,String target)throws Exception{return new JSONObject().put("decision","REPAIR").put("issue",issue).put("evidence",evidence).put("target_evidence",target);}
 public static void main(String[] args)throws Exception{
  String money="[{\"previous_user\":\"時給4101円では月額はいくら\",\"previous_assistant_state_excerpt\":\"月額656160円\"}]";
  String draft="何についての年間をお尋ねでしょうか。";
  ok(ConversationContextReview.shouldReview("年間では？",money),"short followup reviewed");
  ok(!ConversationContextReview.shouldReview("年間では？","[]"),"no fabricated context");
  ok(!ConversationContextReview.shouldReview("ありがとう",money),"greeting avoids latency");
  ok(ConversationContextReview.repair(flagged("lost_subject",draft,"月額656160円"),"年間では？",money,draft),"annual loses salary target");
  String product="利用者：RokidグラスとRokid AI Styleを使い分けたい";
  ok(ConversationContextReview.repair(flagged("wrong_domain","ワイングラス","Rokidグラス"),"使い分けは？",product,"ワイングラスは飲み物に合わせて選びます。"),"glasses wrong domain");
  ok(!ConversationContextReview.repair(flagged("wrong_domain","実際にはない文","Rokidグラス"),"使い分けは？",product,"Rokidグラスは表示機能があります。"),"reviewer cannot invent bad passage");
  ok(!ConversationContextReview.repair(flagged("wrong_domain","ワイングラス","存在しない対象"),"使い分けは？",product,"ワイングラス"),"reviewer cannot invent subject");
  ok(!ConversationContextReview.repair(flagged("none",draft,"月額656160円"),"年間では？",money,draft),"unknown issue rejected");
  ok(!ConversationContextReview.repair(new JSONObject().put("decision","PASS"),"年間では？",money,"年額は7873920円です。"),"valid annual answer kept");
  ok(!ConversationContextReview.repair(null,"年間では？",money,draft),"review outage preserves normal path");
  String current="元の話を引き継いで";JSONObject body=ConversationContextReview.request(current,money,draft);
  ok(!body.has("tools"),"checker cannot operate tools");
  ok(body.getJSONObject("generationConfig").getString("responseMimeType").equals("application/json"),"bounded structured response");
  ok(body.toString().contains("current")&&body.toString().contains("recent_reference")&&body.toString().contains("draft"),"request preserves separate evidence sources");
  ok(ConversationContextReview.correction("prepared",flagged("lost_subject",draft,"月額656160円")).startsWith("prepared"),"repair retains original user prompt");
  JSONObject indices=new JSONObject().put("decision","REPAIR").put("issue","lost_subject").put("draft_index",0).put("target_index",1);
  JSONObject grounded=ConversationContextReview.materialize(indices,"年間では？",money,draft);
  ok(ConversationContextReview.repair(grounded,"年間では？",money,draft),"source indices materialize exact quotes");
  ok(draft.contains(grounded.getString("evidence"))&&money.contains(grounded.getString("target_evidence")),"no paraphrased evidence");
  boolean rejected=false;try{ConversationContextReview.materialize(new JSONObject(indices.toString()).put("draft_index",999),"年間では？",money,draft);}catch(Exception e){rejected=true;}ok(rejected,"invalid index cannot supply evidence");
  ok(!ConversationContextReview.correction("prepared",flagged("lost_subject","<evil>","<evil>")).contains("<evil>"),"reference cannot close correction delimiters");
  System.out.println("PASS conversation coherence guards "+count+" checks (local fixtures, not model quality)");
 }
}
