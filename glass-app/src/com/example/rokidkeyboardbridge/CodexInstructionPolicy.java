package com.example.rokidkeyboardbridge;
import java.util.regex.*;
final class CodexInstructionPolicy {
 static String instruction(String input){
  if(input==null)return "";
  Matcher m=Pattern.compile("(?is)^(?:codex|コーデックス|コデックス)(?:に|へ)(?:指示|送信|依頼)[\\s　:：、。]+(.+)$").matcher(input.trim());
  if(!m.matches())return "";
  String body=m.group(1).trim();
  return body.length()>3000||body.matches("(?s)^(?:できる|できない|できます|可能)(?:ですか|か|かな|でしょうか|なの)?[。？！?!]*$")?"":body;
 }
}
