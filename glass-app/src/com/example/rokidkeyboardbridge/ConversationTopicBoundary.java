package com.example.rokidkeyboardbridge;
/** Explicit topic boundaries only; quotations and negated resets keep their context. */
final class ConversationTopicBoundary {
 static boolean reset(String text){
  if(text==null)return false;String s=text.replace('　',' ').trim();
  s=s.replaceFirst("^(?:じゃあ|では|それじゃ|さて)[、。\\s]*", "");
  if(s.matches("(?is)^(?:別の話(?:題)?|新しい話題)(?:ではなく|ではない|じゃなく|じゃない|にしない|にせず).*")||s.matches("(?is)^(?:会話|文脈)をリセット(?:しない|せず|しなく).*")||s.matches("(?is)^話題を変え(?:ない|ず).*"))return false;
  if(s.matches("(?is)^(?:別の話(?:題)?(?:$|[。、でだにを].*)|話は変わ(?:る|り|っ).*|新しい話題(?:$|[。、でにを].*)|話題を変え(?:て|ます|よう|たい).*|(?:会話|文脈)をリセット(?:$|[。、しす].*)|new topic(?:$|[ :,.].*))"))return true;
  return s.startsWith("ところで")&&!s.matches("(?s).*(?:さっき|先ほど|さきほど|直前|その件|それ|前の|続き|同じ).*");
 }
 private ConversationTopicBoundary(){}
}
