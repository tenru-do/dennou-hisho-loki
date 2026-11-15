package com.example.rokidkeyboardbridge;
/** Presentation only: selecting READ/AMB never starts capture or executes a button. */
final class HudResumePolicy {
 static boolean resetHeadGlance(boolean hudVisible,boolean interactive,boolean alreadyHeadAwake,boolean editing,boolean dialog){return !editing&&!dialog&&(!hudVisible||!alreadyHeadAwake||!interactive);}
 static boolean preferAmbient(boolean active,boolean pending){return active||pending;}
 static String topicLabel(boolean active,boolean paused,boolean pending,boolean topic){return active?(paused?"AMB休止":"AMB中"):pending?"AMB準備":topic?"STOP":"TOPIC";}
 static String topicDescription(boolean active,boolean paused,boolean pending,boolean topic){return active?(paused?"AMB：一時待機中。押すと停止":"AMB：動作中。押すと停止"):pending?"AMB：開始準備中。押すと取り消し":topic?"TOPIC：動作中。押すと停止":"TOPIC・AMB：モードを選ぶ";}
 private HudResumePolicy(){}
}
