package com.example.rokidkeyboardbridge;
public final class ConversationCompletionPolicyTest{
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[]args){
 check(ConversationCompletionPolicy.CONTEXT_WINDOW_MS>17*60*1000,"16 minute pause remains in context");
 String broken="はい、マスター。話の流れは理解しております。空調からゴキブリが侵入してくる件について、ドレンホースが原因ではないかというご質問でしたね。その可能性について、引き続きお話しいただけますでしょうか。";
 check(ConversationCompletionPolicy.needsRepair("話の流れわかってる？",broken,true),"restating question is not answering");
 check(ConversationCompletionPolicy.needsRepair("そうじゃない", "詳しくお話しください。",true),"deferral");
 check(!ConversationCompletionPolicy.needsRepair("話の流れわかってる？", "ドレンホースから侵入する可能性はあります。ただし、配管穴の隙間も候補です。先端の状態を点検し、排水を塞がない防虫対策を確認します。",true),"substantive response preserved");
 check(!ConversationCompletionPolicy.needsRepair("話の流れわかってる？",broken,false),"no invented antecedent");
 check(!ConversationCompletionPolicy.needsRepair("状況を聞いてほしい", "詳しくお話しください。",true),"requested listening preserved");
 check(!ConversationCompletionPolicy.needsRepair("この件を詳しく", "確認方法は三つあります。詳しく説明しましょうか。",true),"not a correction");
 System.out.println("PASS 16 minute context, restatement/deferral repair, substantive answer, missing context and listening exclusions");
 }}