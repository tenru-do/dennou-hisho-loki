package com.example.rokidkeyboardbridge;
/** Keep a bounded recent thread and answer the unfinished question before asking more. */
final class ConversationCompletionPolicy {
 static final long CONTEXT_WINDOW_MS=2L*60L*60L*1000L;
 static final String INSTRUCTION="会話の続きでは、reference_contextにあるユーザーの対象・問題・目的を引き継ぐ。原因候補を『○○かな』と尋ねられたら、その可能性、断定できない点、他の候補、切り分ける確認方法まで今の回答で述べる。すでに問題が提示されているのに、別の一般的な症状を前提に質問を始めない。共感の一文や質問の言い換えだけで終わらない。情報が足りなくても分かる範囲を先に答え、その後に必要な確認だけを一つ添える。『話の流れわかってる？』『そうじゃない』などの訂正には、理解しているとの主張や長い謝罪ではなく、元の実質的な質問へすぐ回答する。『引き続きお話しください』『詳しく見ていきましょうか』『説明してもよいですか』など、すでに頼まれた検討への許可を再度求めて止めない。過去のアシスタントの誤解を正解として引き継がない。外部調査が必要なら実際の検索結果で確認し、未確認の確率や原因を『非常に高い』『確実』と断定しない。新しい話題や条件は現在のユーザー発言を優先する。";
 static String append(String prompt){return prompt+"\n\n<conversation_completion_policy>"+INSTRUCTION+"</conversation_completion_policy>";}
 static boolean needsRepair(String prompt,String answer,boolean reference){
  if(!reference||prompt==null||answer==null)return false;
  boolean correction=prompt.matches("(?s).*(話の流れ|文脈|そうじゃな|そうではな|聞き上手|さっき.*伝え|なぜ.*止め|なんで.*止め|検討するんじゃ|とっとと.*答え).*" );
  if(!correction)return false;
  String body=answer.replaceAll("\\[\\[.*?\\]\\]", "").trim();
  boolean deferral=body.matches("(?s).*(お話しいただ|お話しください|お聞かせ|詳しく.*教え|詳しく.*見ていきましょうか|説明.*よろしい|理解して.*おります).*" );
  boolean substantive=body.matches("(?s).*(可能性(?:は|が|も)?(?:あり|ある|考え)|(?:経路|原因)として|確認(?:するには|方法)|点検|切り分け|一方|結論|対して|×|＝).*" );
  return body.length()<400&&deferral&&!substantive;
 }
 private ConversationCompletionPolicy(){}
}
