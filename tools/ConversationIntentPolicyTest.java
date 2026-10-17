package com.example.rokidkeyboardbridge;
public final class ConversationIntentPolicyTest {
 public static void main(String[] args){int n=0;
 for(String s:new String[]{"眠気がくる","今日は眠い","明日は眠くなりそう","疲れた","寂しい","今日はしんどい","地元に帰って疲れた","もっと詳しく言うと眠気がくる"}){if(!ConversationIntentPolicy.personalState(s)||QueryPolicy.search(s,"イベントを調べて",100)||QueryPolicy.refinement(s))throw new AssertionError(s);n++;}
 for(String s:new String[]{"眠気の原因を調べて","この薬の眠気の副作用について教えて","眠いけど明日の予定を教えて","眠気がくることを覚えて","今週末のイベントを調べて","上野方面にしぼって"}){if(ConversationIntentPolicy.personalState(s))throw new AssertionError(s);n++;}
 if(!QueryPolicy.search("眠気の原因を調べて"))throw new AssertionError("Research blocked");
 if(!QueryPolicy.search("上野方面にしぼって","今週末のイベントを調べて",100))throw new AssertionError("Refinement blocked");
 System.out.println("PASS "+(n+2)+" conversational states,topic switches,explicit research/schedule/memory requests");
 }
}
