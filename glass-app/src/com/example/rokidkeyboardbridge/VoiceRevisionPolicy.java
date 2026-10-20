package com.example.rokidkeyboardbridge;
final class VoiceRevisionPolicy {
 static boolean correction(String text){if(text==null)return false;String s=text.trim();return ConversationCorrections.explicit(s)||s.matches("(?s)^(?:いや|違う|ちがう)[、,。\\s]+(?:そうじゃなくて|そうではなく|訂正|さっきの|今の).*");}
 static String carry(String original,String correction){String a=original==null?"":original.trim(),b=correction==null?"":correction.trim();return a.isEmpty()||a.equals(b)?b:a+"\n"+b;}
 private VoiceRevisionPolicy(){}
}
