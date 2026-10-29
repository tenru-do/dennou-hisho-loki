package com.example.rokidkeyboardbridge;
/** TOPIC starts explicitly and must not stay blocked by a dormant VOICE listener. */
final class TopicRunGuard {
 static boolean handoff(boolean enabling,boolean voiceLoop,boolean recording,boolean recognizer){return enabling&&(voiceLoop||recording||recognizer);}
 static String blocked(boolean foreground,boolean menu,boolean navigation,boolean conversation,boolean request,boolean capture){
  if(!foreground)return "BACKGROUND";if(menu)return "MENU";if(navigation)return "NAVIGATION";
  if(conversation||request)return "CONVERSATION";if(capture)return "CAPTURING";return "";
 }
 private TopicRunGuard(){}
}
