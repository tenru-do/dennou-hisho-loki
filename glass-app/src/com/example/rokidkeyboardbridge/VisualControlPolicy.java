package com.example.rokidkeyboardbridge;
/** Direct visual controls only; never interpret quoted conversation as an action. */
final class VisualControlPolicy {
 static String clean(String text){return VisualConversationPlan.clean(text).replaceAll("[。！!\\s]+$","");}
 static boolean diagnose(String text){return clean(text).matches("今の(?:姿|表示|動き)がおかしい|今の不具合を記録");}
 static boolean hold(String text){return clean(text).matches("そのまま(?:で|でいて|にして)?|その姿勢(?:のまま|を保って)|今の姿勢を保持して");}
 static boolean undo(String text){return clean(text).matches("さっきの(?:姿|姿勢|見た目)に戻って|一つ前の(?:姿|姿勢|見た目)に戻して|ひとつ前の(?:姿|姿勢|見た目)に戻して|元の姿勢に戻って");}
 static int simplePosture(String text){String t=clean(text);return t.matches("(?:ちょっと|少し|もう一度)?(?:座って|立って|寝そべって|横になって)(?:ください|ね)?")?AvatarActionDirector.posture(t):-1;}
 static boolean changesVisual(String text){return ApprovedQuietPosePolicy.sofa(text)||WarmPresencePolicy.command(text)>0||hold(text)||undo(text)||AvatarActionDirector.stop(text)||EmbodiedScenePolicy.visualCommand(text)||VisualConversationPlan.resolve(text,0,"").kind==VisualConversationPlan.ACTION;}
 static String visibleReply(int posture){return posture==0?"立つ姿を表示しています。":posture==1?"座る姿を表示しています。":"寝姿を表示しています。";}
}
