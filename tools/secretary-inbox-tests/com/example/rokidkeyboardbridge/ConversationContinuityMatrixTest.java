package com.example.rokidkeyboardbridge;
public class ConversationContinuityMatrixTest{
 static int n;static void ok(boolean b,String label){if(!b)throw new AssertionError(label);n++;}
 public static void main(String[]args){
 String[] resets={"別の話。さっきの質問とは関係ないけど、明日の準備をしよう","話題を変えて。前の話は終わり","会話をリセット。年間では？","新しい話題で、前の質問は後にして","ところで、今日の天気は？","じゃあ、別の話をしよう","　別の話。前の条件は使わない","new topic: ignore the previous question"};
 for(String s:resets){ok(ConversationTopicBoundary.reset(s),"reset "+s);ok(!ConversationFollowupPolicy.referencesPrevious(s),"no old followup "+s);}
 String[] continuations={"話題を変えないで。さっき伝えたはず","話題を変えずに、とっとと提案して","別の話ではなく、さっきの質問の続き","会話をリセットしないで。前の質問に答えて","『別の話』と言ったわけじゃない。前の質問の続き","ところで、さっきの質問の年間では？"};
 for(String s:continuations){ok(!ConversationTopicBoundary.reset(s),"keep "+s);ok(ConversationFollowupPolicy.referencesPrevious(s),"followup "+s);}
 for(String s:new String[]{"鍵を忘れた","スマホを忘れた","昨日の予定を忘れちゃった"})ok(!ConversationFollowupPolicy.referencesPrevious(s),"personal forgotten item "+s);
 for(String s:new String[]{"忘れた？","前の質問忘れた？","年間では？","手取りだと？","さっきの件を忘れた？"})ok(ConversationFollowupPolicy.referencesPrevious(s),"retained antecedent "+s);
 ok(ConversationTaskPolicy.continuesTask("話題を変えないで。さっき伝えたはず"),"task remains");ok(!ConversationTaskPolicy.continuesTask("別の話。とっとと提案して"),"explicit reset wins");
 System.out.println("PASS "+n+" topic boundary/followup matrix checks");
 }}
