package com.example.rokidgeminisecretary;
import org.json.*;
public class SecretaryInboxPresentationTest{
 static int n;static void ok(boolean value,String label){if(!value)throw new AssertionError(label);n++;}
 static JSONObject card(String state)throws Exception{return new JSONObject().put("state",state).put("id","fixture").put("title","家族と確認するイベント");}
 public static void main(String[]args)throws Exception{
  JSONObject pending=card("pending");ok(SecretaryInboxPresentation.decorate(pending,1000).getString("displayStage").equals("pending"),"plain pending");ok(!pending.has("displayStage"),"projection never mutates source");
  JSONObject review=card("pending").put("review",new JSONObject()).put("reviewToken","private-token").put("reviewAt",1000);ok(SecretaryInboxPresentation.decorate(review,2000).getString("displayStage").equals("review_wait"),"live review distinguished");ok(SecretaryInboxPresentation.decorate(review,601000).getString("displayStage").equals("review_wait"),"matches exact confirm expiry boundary");ok(SecretaryInboxPresentation.decorate(review,601001).getString("displayStage").equals("review_expired"),"expired review explicit");ok(SecretaryInboxPresentation.decorate(review,999).getString("displayStage").equals("review_expired"),"future review fails closed");
  JSONObject stale=card("pending").put("reviewAt",1000);ok(SecretaryInboxPresentation.decorate(stale,2000).getString("displayStage").equals("pending"),"invalidated review time alone not approval");for(String state:new String[]{"executing","unknown","unrecognized"})ok(SecretaryInboxPresentation.decorate(card(state),2000).getString("displayStage").equals("needs_check"),"unconfirmed state "+state);
  ok(!SecretaryInboxPresentation.active(card("completed"))&&!SecretaryInboxPresentation.active(card("cancelled")),"closed hidden from active view");ok(SecretaryInboxPresentation.active(card("unknown")),"unknown never hidden as completed");ok(SecretaryInboxPresentation.decorate(card("completed"),2000).getString("stateLabel").equals("登録完了"),"completion label");ok(SecretaryInboxPresentation.decorate(card("cancelled"),2000).getString("stateLabel").equals("取消"),"cancel label");
  System.out.println("PASS inbox read-only presentation "+n+" checks");
 }
}
