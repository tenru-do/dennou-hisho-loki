package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyRearStopTest {
 public static void main(String[]args){
  for(String text:new String[]{"おやめください。","私は触らないでと拒みます。","振り返って、やめてください。","い、いや…っ…！"}){
   FullBodySpeechStance.Cue c=FullBodySpeechStance.classify(text);
   if(c==null||!FullBodyRearStopPolicy.eligible(c.kind,false,text))throw new AssertionError(text);
   if(FullBodyRearStopPolicy.eligible(c.kind,true,text))throw new AssertionError("seated must not stand");
  }
  for(String text:new String[]{null,"","彼女はやめてくださいと言いました。","『やめてください』という言葉について説明します。","拒むことはありません。","もし困ったら、やめてくださいと言います。","ありがとうございます。","もう大丈夫です。"})
   if(FullBodyRearStopPolicy.eligible(REFUSE,false,text))throw new AssertionError("indirect/ended refusal: "+text);
  if(FullBodyRearStopPolicy.eligible(JOY,false,"やめてください。"))throw new AssertionError("semantic boundary");
  long[] times={-10,0,239,240,499,500,1149,1150,1409,1410,1649,1650,99999};
  int[] frames={1,1,1,0,0,2,2,0,0,1,1,0,0};
  for(int i=0;i<times.length;i++){
   if(FullBodyRearStopPolicy.frame(times[i])!=frames[i])throw new AssertionError("timing "+times[i]);
   float blend=FullBodyRearStopPolicy.blend(times[i]);if(blend<0||blend>1)throw new AssertionError("blend");
  }
  if(FullBodyRearStopPolicy.blend(240)!=0||FullBodyRearStopPolicy.blend(330)!=1)throw new AssertionError("smooth boundary");
  for(int t=0;t<10000;t++)if(FullBodyRearStopPolicy.frame(t)<0||FullBodyRearStopPolicy.frame(t)>=3)throw new AssertionError("asset range");
  System.out.println("PASS rear stop: direct refusal, indirect/negative/recovery guards, seated preservation, one-shot sequence and crossfade");
 }
}
