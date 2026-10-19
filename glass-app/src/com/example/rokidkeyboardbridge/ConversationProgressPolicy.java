package com.example.rokidkeyboardbridge;
/** Content-free progress labels; never automatically resubmit a failed user turn. */
final class ConversationProgressPolicy {
 static final String PREPARE="会話の引き継ぎを整理中",ANSWER="Geminiの回答待ち",REVIEW="回答の文脈を確認中",REPAIR="回答を調整中";
 static String failure(String stage,Throwable error,boolean safety){
  return failure(stage,error,safety,ConversationFailurePolicy.code(error,0,"",safety));
 }
 static String failure(String stage,Throwable error,boolean safety,String code){
  if(error instanceof LocalConversationStop)return error.getMessage();
  if(safety)return "安全判定で停止。このやり取りは履歴に保存しません";
  return "停止："+ConversationFailurePolicy.stageLabel(ConversationFailurePolicy.stage(stage))+"。"
    +ConversationFailurePolicy.detail(ConversationFailurePolicy.validCode(code)?code:"PROCESSING");
 }
 private ConversationProgressPolicy(){}
}
