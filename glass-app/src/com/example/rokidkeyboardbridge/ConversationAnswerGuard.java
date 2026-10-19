package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
/** Check before any function call, history write, speech or completion receipt. */
final class ConversationAnswerGuard {
 static final class Incomplete extends java.io.IOException {
  final String reason;
  Incomplete(String finish){super("SAFETY_STOP".equals(finish)?"SAFETY_STOP":"回答の完了を確認できませんでした（自動再送なし）");reason=finish;}
 }
 static void requireComplete(JSONObject candidate)throws Incomplete{
  String finish=candidate==null?"":candidate.optString("finishReason");
  if("STOP".equals(finish))return;
  String reason="MAX_TOKENS".equals(finish)?"OUTPUT_LIMIT":NormalRequestContextPolicy.safetyStop(finish)?"SAFETY_STOP":"INCOMPLETE_RESPONSE";
  throw new Incomplete(reason);
 }
 private ConversationAnswerGuard(){}
}
