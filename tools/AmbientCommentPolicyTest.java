package com.example.rokidkeyboardbridge;
public final class AmbientCommentPolicyTest {
 public static void main(String[]args){
  if(AmbientCommentPolicy.accept("少し寂しいです。","")||AmbientCommentPolicy.accept("SKIP","")||AmbientCommentPolicy.accept("構ってください","")||AmbientCommentPolicy.accept("この後の予定を確認します。","この後の予定を確認します！"))throw new AssertionError("Unhelpful or repeated idle speech");
  if(!AmbientCommentPolicy.accept("16時30分になりました。","")||AmbientCommentPolicy.due(3600001,1,0)||AmbientCommentPolicy.due(3600001,1,22)||AmbientCommentPolicy.due(3600000,1,12)||!AmbientCommentPolicy.due(3600001,1,12))throw new AssertionError("Time and cooldown");
  System.out.println("PASS quiet hours, persistent hourly cooldown, loneliness and repeat suppression, useful comment");
 }
}
