package com.example.rokidkeyboardbridge;
public final class FullBodyHeavyBreathTest {
 public static void main(String[]args){
  for(String text:new String[]{"私は息を切らしています。","息が上がっています。","呼吸が荒いです。"}){
   FullBodySpeechStance.Cue c=FullBodySpeechStance.classify(text);
   if(c==null||!FullBodyHeavyBreathPolicy.eligible(c.kind,false))throw new AssertionError(text);
   if(FullBodyHeavyBreathPolicy.eligible(c.kind,true))throw new AssertionError("seated must remain seated");
  }
  for(String text:new String[]{"彼女は息を切らしています。","息は呼吸が荒いという言葉について説明します。","呼吸が荒くありません。","呼吸を整えます。","ありがとうございます。"}){
   FullBodySpeechStance.Cue c=FullBodySpeechStance.classify(text);
   if(c!=null&&FullBodyHeavyBreathPolicy.eligible(c.kind,false))throw new AssertionError("false positive: "+text);
  }
  int[] times={-1,0,199,200,449,450,599,600,999,1000,1200};
  int[] expected={0,0,0,1,1,0,0,2,2,0,1};
  for(int i=0;i<times.length;i++)if(FullBodyHeavyBreathPolicy.frame(times[i])!=expected[i])throw new AssertionError("phase "+times[i]);
  System.out.println("PASS heavy breathing: first-person cues, third-party/negation/recovery guards, seated preservation, repeated timing");
 }
}
