package com.example.rokidkeyboardbridge;
/** Confirmation race cases; no UI input, attachments, network or user data. */
public final class DraftRestorePolicyTest {
 static int checks;static void ok(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  String shown="合成の下書き";ok(DraftRestorePolicy.reason(shown,shown,false,false).isEmpty());
  ok(!DraftRestorePolicy.reason(shown,"確認中に追記した本文",false,false).isEmpty());
  ok(!DraftRestorePolicy.reason(shown,shown,true,false).isEmpty());
  ok(!DraftRestorePolicy.reason(shown,shown,false,true).isEmpty());
  ok(!DraftRestorePolicy.reason(null,shown,false,false).isEmpty());
  String[] drafts={"",shown,"同じに見えるが末尾に空白 ","違う本文"};
  for(String initial:drafts)for(String current:drafts)ok(DraftRestorePolicy.reason(initial,current,false,false).isEmpty()==initial.equals(current));
  ok(!DraftRestorePolicy.reason(shown,shown,true,true).isEmpty());
  System.out.println("PASS "+checks+" confirmation race / attachment / processing checks; external requests=0");
 }
}
