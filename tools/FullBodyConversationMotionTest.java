package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyConversationMotionTest {
 public static void main(String[] args){
  if(FullBodyConversationMotion.kind(SURPRISE,false,true)!=0)throw new AssertionError("seated surprise");
  if(FullBodyConversationMotion.kind(JOY,true,true)!=1)throw new AssertionError("seated laughter");
  if(FullBodyConversationMotion.kind(JOY,false,true)!=1)throw new AssertionError("seated restrained smile without laughter");
  if(FullBodyConversationMotion.kind(SURPRISE,false,false)!=-1)throw new AssertionError("standing unchanged");
  if(FullBodyConversationMotion.kind(REFUSE,false,true)!=-1)throw new AssertionError("refusal preempts laughter");
  for(String text:new String[]{"お、おやめください…っ…","そのようなご要望にはお応えできかねます","い、いや…っ…！","わたくしは「お、おやめください」と制止します。"}){
   FullBodySpeechStance.Cue c=FullBodySpeechStance.classify(text);
   if(c==null||c.kind!=REFUSE)throw new AssertionError("boundary: "+text);
  }
  if(FullBodySpeechStance.classify("涙を流す").kind!=CRY)throw new AssertionError("existing crying reaction");
  if(FullBodyConversationMotion.kind(CRY,false,false)!=2)throw new AssertionError("cry motion");
  if(FullBodyConversationMotion.pose(2,4)!=289)throw new AssertionError("same hand returns through existing intermediate");
  if(FullBodySpeechStance.thoughtMotion("彼女は思い出そうとしています。")!=-1)throw new AssertionError("third party recall");
  if(FullBodyConversationMotion.kind(THINK,false,false,"少し虚空を見てぼーっとしています。")!=3)throw new AssertionError("daydream");
  if(FullBodyConversationMotion.kind(THINK,false,false,"記憶をたどり、思い出そうとしています。")!=-1)throw new AssertionError("removed recall");
  if(FullBodyConversationMotion.kind(THINK,false,false,"調べます。")!=-1)throw new AssertionError("routine thought must not kneel");
  if(FullBodyConversationMotion.kind(REFUSE,false,false,"思い出そうとしています。")!=-1)throw new AssertionError("boundary priority");
  if(FullBodySpeechStance.thoughtMotion("思い出すように首を横に振る。")!=-1)throw new AssertionError("removed head shake");
  if(FullBodyConversationMotion.continues(4,THINK,false))throw new AssertionError("removed motion continues");
  int previous=-1;for(int ms=0;ms<20000;ms+=10){int f=FullBodyConversationMotion.frame(ms);if(f<previous||f<0||f>5)throw new AssertionError("non-looping motion");previous=f;}
  for(int kind=0;kind<5;kind++)for(int id:FullBodyConversationMotion.frames(kind))if(!FullBodyFaceAnimator.supports(id))throw new AssertionError("mouth/blink landmarks "+id);
  System.out.println("PASS seated motion: surprise/laugh context, refusal, standing preservation, one-shot timing, all face landmarks");
 }
}
