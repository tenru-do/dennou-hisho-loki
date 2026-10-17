package com.example.rokidkeyboardbridge;
public final class GroundingNotifyTest { public static void main(String[] args){
 if(SearchGroundingPolicy.requiresSources(false,0))throw new AssertionError("Ordinary reply rejected");
 if(!SearchGroundingPolicy.requiresSources(true,0)||!SearchGroundingPolicy.requiresSources(false,1))throw new AssertionError("Ungrounded search accepted");
 long now=1000000;if(!CodexNotificationPolicy.fresh(now,now)||CodexNotificationPolicy.fresh(now-300001,now)||CodexNotificationPolicy.fresh(now+30001,now))throw new AssertionError("Freshness");
 if(!CodexNotificationPolicy.safe("Codexの作業が完了しました。")||CodexNotificationPolicy.safe("Risk level: low")||CodexNotificationPolicy.safe("{\"risk\":\"low\"}"))throw new AssertionError("Internal notification");
 String sleep="昨夜は一時に寝て、今朝は八時に起きた。明日も早起きして、知人を駅へ送る予定。";
 if(!ConversationIntentPolicy.personalState(sleep)||QueryPolicy.search(sleep,"イベントを調べて",100))throw new AssertionError("Sleep follow-up");
 System.out.println("PASS grounding availability/execution,notification expiry/internal messages,exact sleep narrative");
 }}