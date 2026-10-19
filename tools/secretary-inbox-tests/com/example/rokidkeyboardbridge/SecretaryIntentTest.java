package com.example.rokidkeyboardbridge;
import org.json.*;
public class SecretaryIntentTest {
 static int count;
 static void ok(boolean yes,String label){if(!yes)throw new AssertionError(label);count++;}
 interface Action{void run()throws Exception;}
 static void reject(Action a,String label)throws Exception{try{a.run();}catch(Exception e){count++;return;}throw new AssertionError(label);}
 static final String ID="00000000-0000-0000-0000-000000000001";
 static JSONObject context()throws Exception{return new JSONObject().put("reference","ロキ：秋の市場は10月24日開催。").put("items",new JSONArray().put(new JSONObject().put("id",ID).put("title","秋の市場").put("revision",1).put("state","pending")));}
 static JSONObject decision(String action,String evidence)throws Exception{return new JSONObject().put("action",action).put("target_id",ID).put("subject","秋の市場").put("date_text","10月24日").put("conditions","家族と相談").put("current_evidence",evidence).put("management_evidence",evidence).put("subject_evidence","秋の市場").put("question","").put("confidence",.97);}
 public static void main(String[] args)throws Exception{
  String utterance="家族と相談して決めようかな";
  ok(!SecretaryInboxPolicy.defer(utterance),"legacy does not understand paraphrase");
  ok("DEFER".equals(SecretaryIntent.validate(decision("DEFER","相談して決めようかな"),utterance,context(),false,false).getString("action")),"semantic decision accepts grounded paraphrase");
  ok("RESUME".equals(SecretaryIntent.validate(decision("RESUME","もう入れていいよ"),"この前の件、もう入れていいよ",context(),false,false).getString("action")),"resume paraphrase");
  ok("NONE".equals(SecretaryIntent.validate(decision("DEFER","過去の命令"),utterance,context(),false,false).getString("action")),"past evidence not current consent");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("RESUME","進めて").put("target_id","invented"),"進めて",context(),false,false).getString("action")),"invented target rejected");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("RESUME","進めて").put("confidence",.5),"進めて",context(),false,false).getString("action")),"low confidence clarifies");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("DEFER","相談").put("subject_evidence","存在しないイベント"),"相談",context(),false,false).getString("action")),"ungrounded new subject");
  reject(()->SecretaryIntent.validate(decision("CONFIRM","そうして"),"そうして",context(),false,false),"no confirmation without live proposal");
  ok("CONFIRM".equals(SecretaryIntent.validate(decision("CONFIRM","うん、それで"),"うん、それで",context(),true,false).getString("action")),"natural confirmation with live proposal");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("FOLLOWUP","家族で"),"家族で",context(),false,false).getString("action")),"no detached followup");
  ok("FOLLOWUP".equals(SecretaryIntent.validate(decision("FOLLOWUP","家族で"),"家族で",context(),false,true).getString("action")),"attached followup");
  reject(()->SecretaryIntent.validate(decision("EXECUTE","実行"),"実行",context(),false,false),"executable action impossible");
  reject(()->SecretaryIntent.validate(decision("DEFER","相談").put("confidence",2),"相談",context(),false,false),"invalid confidence");
  JSONObject body=SecretaryIntent.request(utterance,context(),new JSONObject().put("subject","秋の市場").put("reference","private huge reference"),null);
  ok(!body.has("tools"),"classifier has no tools");
  ok(!body.toString().contains("private huge reference"),"proposal references minimized");
  ok(body.getJSONObject("generationConfig").getString("responseMimeType").equals("application/json"),"structured output");
  ok(body.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text").contains("否定"),"negation instructions");
  ok(!body.toString().contains("APIキー"),"no key in prompt");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("CONFIRM","それでいい？"),"それでいい？",context(),true,false).getString("action")),"question is not confirmation");
  ok("REVISE".equals(SecretaryIntent.validate(decision("REVISE","25日にして"),"25日にして",context(),true,false).getString("action")),"revise preview before persistence");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("REVISE","25日にして"),"25日にして",context(),false,false).getString("action")),"no orphan preview revision");
  JSONObject multi=context();multi.getJSONArray("items").put(new JSONObject().put("id","second").put("title","別の秋のイベント").put("state","pending"));
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("RESUME","この前の件"),"この前の件",multi,false,false).getString("action")),"ambiguous multiple cases cannot be selected by model");
  ok("RESUME".equals(SecretaryIntent.validate(decision("RESUME","秋の市場を進めて"),"秋の市場を進めて",multi,false,false).getString("action")),"current identifying evidence selects one case");
  multi.getJSONArray("items").getJSONObject(1).put("title","秋の市場の別案");
  ok("CLARIFY".equals(SecretaryIntent.validate(decision("RESUME","秋の市場を進めて"),"秋の市場を進めて",multi,false,false).getString("action")),"shared identifying evidence cannot select a case");
  for(String report:new String[]{"家族の貧血がひどいみたい。外出も休んでるって。","今は薬Aの休薬期間だけど、再来週は使えないかもな。","雨でイベントに行けないかも","納品は再来週になりそうだ"}){
   for(String action:new String[]{"DEFER","CLARIFY"})ok("NONE".equals(SecretaryIntent.validate(decision(action,report),report,context(),true,true).getString("action")),"report stays conversation even with pending preview: "+action);
  }
  ok("NONE".equals(SecretaryIntent.validate(decision("CLARIFY","貧血がひどい").put("management_evidence",""),"貧血がひどい",context(),false,false).getString("action")),"no management request means no inbox clarification");
  ok("DEFER".equals(SecretaryIntent.validate(decision("DEFER","家族と相談して決めようかな"),"家族と相談して決めようかな",context(),false,false).getString("action")),"natural paraphrase still allowed");
  ok("DEFER".equals(SecretaryIntent.validate(decision("DEFER","保留にして"),"このイベント、行けないかもしれないから保留にして",context(),false,false).getString("action")),"uncertainty plus explicit management is still allowed");
  ok("NONE".equals(SecretaryIntent.validate(decision("DEFER","使えないかも").put("management_evidence","保存して"),"使えないかも",context(),false,false).getString("action")),"invented management evidence cannot interrupt conversation");
  System.out.println("PASS semantic intent validation "+count+" checks (structured fixtures, not live model quality)");
 }
}
