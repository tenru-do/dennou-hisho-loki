package com.example.rokidkeyboardbridge;
public final class ConversationStopPolicy {
 public static boolean matches(String requested,String active,String normal,String secretary){return TrackedWebRequest.validId(requested)&&requested.equals(active)&&requested.equals(normal)&&(secretary==null||secretary.isEmpty());}
 private ConversationStopPolicy(){}
}
