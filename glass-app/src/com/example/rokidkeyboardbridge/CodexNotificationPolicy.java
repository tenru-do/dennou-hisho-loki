package com.example.rokidkeyboardbridge;
final class CodexNotificationPolicy {
 static boolean kind(String value){return "completed".equals(value)||"question".equals(value)||"status".equals(value);}
 static boolean fresh(long issued,long now){return issued>0&&now-issued>=-30000&&now-issued<=300000;}
 static boolean safe(String message){return message!=null&&!message.trim().isEmpty()&&!message.trim().startsWith("{")&&!message.matches("(?is).*(risk|auto.?review|approvals_reviewer|sandbox_permissions).* ".trim());}
}
