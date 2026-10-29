package com.example.rokidkeyboardbridge;
/** One local phase/emotion authority; navigation and direct commands outrank optional life. */
final class CharacterChoreography {
 static final int IDLE=0,LISTEN=1,THINK=2,SPEAK=3,CLARIFY=4,NAV=5,READ=6;
 static volatile int phase,scene;static volatile float gain=1;static volatile int talkLevel=2,navLevel=0,readLevel=1;
 private static long phaseAt;private static int clarification;
 static void update(boolean navigation,boolean recording,boolean recognizing,boolean waiting,boolean speaking,long now){int next=navigation?NAV:recording||recognizing?LISTEN:waiting?THINK:speaking?SPEAK:IDLE;if(phase==CLARIFY&&next==IDLE&&now>=phaseAt&&now-phaseAt<2600)next=CLARIFY;if(next!=phase){phase=next;phaseAt=now;}scene=navigation?1:!speaking&&!waiting&&!recording&&!recognizing?2:0;int level=scene==1?navLevel:scene==2?readLevel:talkLevel;gain=level<=0?0:level==1?.45f:1f;}
 static void clarify(long now){phase=CLARIFY;phaseAt=now;clarification++;}
 static boolean clarifying(long now){return phase==CLARIFY&&now>=phaseAt&&now-phaseAt<2600;}
 static int clarificationVariant(){return clarification%2;}
 static float lead(long age){return age<0||age>=650?0:(float)Math.sin(Math.PI*age/650.0);}
 static boolean emotionalPriority(FullBodyPoseCatalog.Kind kind){return kind!=null&&(ConversationVisualCatalog.priority(kind)||ConversationVisualCatalog.restReaction(kind)==RestConversationReaction.CARE||kind==FullBodyPoseCatalog.Kind.REFUSE||kind==FullBodyPoseCatalog.Kind.ANGER);}
 static boolean optionalAllowed(boolean quiet,boolean manual,boolean loading,FullBodyPoseCatalog.Kind kind){return !quiet&&!manual&&!loading&&!emotionalPriority(kind);}
 static boolean clarificationText(String t){return t!=null&&(t.contains("聞き取れません")||t.contains("音声認識エラー")||t.contains("もう一度お話し")||t.contains("どちらの意味"));}
 private CharacterChoreography(){}
}
