package com.example.rokidkeyboardbridge;
public final class CodexInstructionPolicyTest {
 public static void main(String[] args){
  if(!CodexInstructionPolicy.instruction("Codexに指示、通知を直して").equals("通知を直して"))throw new AssertionError("explicit instruction");
  if(!CodexInstructionPolicy.instruction("コーデックスへ送信：テストです").equals("テストです"))throw new AssertionError("voice Japanese name");
  if(!CodexInstructionPolicy.instruction("Codexに指示、どうすればこのバグを直せる？").equals("どうすればこのバグを直せる？"))throw new AssertionError("explicit question for Codex");
  for(String s:new String[]{"Codexに指示できる？","ここからCodexに指示できないかな","Codexに指示、できますか？","眠気がくる","年間では？","Codexに指示、"})if(!CodexInstructionPolicy.instruction(s).isEmpty())throw new AssertionError("unintended dispatch "+s);
  for(String kind:new String[]{"question","completed","status"})if(!CodexNotificationPolicy.kind(kind))throw new AssertionError(kind);
  for(String kind:new String[]{"risk","approval","unknown",""})if(CodexNotificationPolicy.kind(kind))throw new AssertionError(kind);
  if(CodexNotificationPolicy.safe("Risk low sandbox_permissions"))throw new AssertionError("internal review leaked");
  if(CodexNotificationPolicy.fresh(1,900000))throw new AssertionError("stale notification");
  System.out.println("PASS explicit instructions, capability questions, supported notification kinds, internal/stale filtering");
 }
}
