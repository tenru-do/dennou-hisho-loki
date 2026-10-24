package com.example.rokidkeyboardbridge;
final class ConversationFirstTurnPolicy {
 static boolean staleFailure(String failed,String shown,boolean active){return !active&&failed!=null&&!failed.isEmpty()&&failed.equals(shown);}
 private ConversationFirstTurnPolicy(){}
}
