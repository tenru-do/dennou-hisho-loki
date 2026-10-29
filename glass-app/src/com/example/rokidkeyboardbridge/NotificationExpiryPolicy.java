package com.example.rokidkeyboardbridge;
/** Only releases the latest transient notice, never another answer or active work. */
final class NotificationExpiryPolicy {
 static final int IGNORE=0,RETRY=1,RELEASE=2;
 static int decide(boolean latest,boolean busy,boolean sameText,boolean empty,long age){
  if(!latest||age<18000||(!sameText&&!empty))return IGNORE;
  if(busy)return age<120000?RETRY:IGNORE;
  return RELEASE;
 }
 private NotificationExpiryPolicy(){}
}
