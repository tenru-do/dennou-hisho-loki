package com.example.rokidkeyboardbridge;
public final class ConversationReplyPolicyTest {
 public static void main(String[] args){
  String a=FullBodySemanticReaction.decode("ぬ゙、ここから直接送る機能は未実装です。[[LISTEN:1:DEFAULT]]");if(!a.equals("ここから直接送る機能は未実装です。"))throw new AssertionError(a);if(FullBodySemanticReaction.find(a)==null)throw new AssertionError("reaction lost");
  String b=FullBodySemanticReaction.decode("説明が足りませんでした。[[WORRY:2:DEFAULT]]");if(b.contains("[[")||FullBodySemanticReaction.find(b)==null)throw new AssertionError(b);
  String c=FullBodySemanticReaction.decode("マスター、確認します。[[LOKI_REACTION:EXPLAIN:1:EXPLAIN]]");if(c.contains("[["))throw new AssertionError(c);
  if(!FullBodySemanticReaction.decode("本文。[[LISTEN:1:").equals("本文。"))throw new AssertionError("partial leak");
  if(!ConversationReplyPolicy.clean("「ぬ゙」という文字です。").equals("「ぬ゙」という文字です。"))throw new AssertionError("quoted text changed");
  if(!ConversationReplyPolicy.clean("ぬ゙にご不快な思いをさせました。").startsWith("マスターに"))throw new AssertionError("vocative");
  if(!FullBodySemanticReaction.decode("[[USER_NOTE]]").equals("[[USER_NOTE]]"))throw new AssertionError("unrelated tag removed");
  System.out.println("PASS bare/canonical/partial reaction tags,metadata preserved,vocatives,quoted and unrelated text");
 }
}