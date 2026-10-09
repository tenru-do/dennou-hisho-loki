package com.example.rokidkeyboardbridge;
public final class AmbientDisplayPolicyTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  for(int i=0;i<3;i++)check(!AmbientScenePolicy.scene(i),"legacy preference preserved");
  for(int i=3;i<=5;i++){
   check(AmbientScenePolicy.scene(i),"new source selectable");
   check(AmbientScenePolicy.microphone(i)==(i!=4),"microphone source");
   check(AmbientScenePolicy.camera(i)==(i!=3),"camera source");
  }
  FullBodySpeechStance speech=new FullBodySpeechStance();speech.scene("ロキにセクハラして","やめてください");
  speech.observed(new FullBodySpeechStance.Cue(FullBodyPoseCatalog.Kind.JOY,1));speech.begin("いいね");speech.pose("いいね",4);
  check(speech.kind()==FullBodyPoseCatalog.Kind.JOY&&speech.laughing(),"observed joy survives neutral wording and TTS begin");
  speech.observed(null);speech.scene("天気を教えて","調べます");speech.begin("調べます");speech.pose("調べます",4);check(!speech.laughing(),"normal conversation clears observed laugh");
  int blink=0,closed=0,transitions=0;
  for(int i=0;i<FullBodyRealisticFeatures.ALL.length;i++){
   float[] f=FullBodyFaceAnimator.features(i);check(f!=null&&f.length==19,"landmarks "+i);
   if(FullBodyFaceAnimator.transition(i)){transitions++;continue;}
   if(f[7]>0||f[13]>0)blink++;else closed++;
  }
  check(transitions==36&&blink+closed==FullBodyRealisticFeatures.ALL.length-36,"all non-transition frames covered");
  check(FullBodyFaceAnimator.blink(4600)==1&&FullBodyFaceAnimator.blink(0)==0,"blink timing");
  System.out.println("PASS AMB source selection, legacy preservation, observed laughter and conversation reset; blink coverage="+blink+" naturally closed/occluded="+closed+" transitions="+transitions);
 }
}
