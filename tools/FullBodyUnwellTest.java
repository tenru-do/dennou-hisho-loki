package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyUnwellTest {
 public static void main(String[]args){
  for(String text:new String[]{"私は少し気分が悪いです。","体調が悪いです。","吐き気がします。","吐き気を感じています。"}){
   if(!FullBodyUnwellPolicy.eligible(null,true,text))throw new AssertionError(text);
   if(FullBodyUnwellPolicy.eligible(null,false,text))throw new AssertionError("standing posture changed");
   if(FullBodyUnwellPolicy.eligible(REFUSE,true,text))throw new AssertionError("refusal priority");
  }
  for(String text:new String[]{null,"","彼女は気分が悪いです。","友人は体調が悪いです。","患者は吐き気がします。","『気分が悪いです』という言葉について説明します。","私は気分が悪くありません。","吐き気がしません。","もし体調が悪いですと言ったら休みます。","あなたは吐き気がしますか？","呼吸を整えます。","ありがとうございます。"})
   if(FullBodyUnwellPolicy.eligible(TIRED,true,text))throw new AssertionError("false positive "+text);
  if(!FullBodyUnwellPolicy.eligible(null,true,"気分が悪くありません。体調が悪いです。"))throw new AssertionError("independent affirmative clause");
  for(long t:new long[]{-1,0,899})if(FullBodyUnwellPolicy.frame(t)!=0)throw new AssertionError("cover phase");
  for(long t:new long[]{900,1080,100000})if(FullBodyUnwellPolicy.frame(t)!=1)throw new AssertionError("steady phase must not loop");
  if(FullBodyUnwellPolicy.blend(900)!=0||FullBodyUnwellPolicy.blend(990)!=0.5f||FullBodyUnwellPolicy.blend(1080)!=1)throw new AssertionError("crossfade");
  System.out.println("PASS ordinary unwellness: seated/direct cues, standing/refusal/third-party/negation/hypothetical guards, one-shot recovery and blending");
 }
}
