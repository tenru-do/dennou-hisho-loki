package com.example.rokidkeyboardbridge;
/** Meaning cues mapped to the existing portrait expression registry. */
final class PortraitSemanticReaction {
 static FullBodySpeechStance.Cue resolve(String prompt,String answer){
  FullBodySpeechStance.Cue scene=FullBodyScenePolicy.evaluate(prompt,answer),wording=FullBodySpeechStance.classify(answer);
  FullBodySemanticReaction.Reaction model=FullBodySemanticReaction.find(answer);
  if(scene!=null)return scene;
  if(wording!=null&&(wording.kind==FullBodyPoseCatalog.Kind.REFUSE||wording.kind==FullBodyPoseCatalog.Kind.ANGER))return wording;
  return model!=null?model.cue:wording;
 }
 static int expression(FullBodySpeechStance.Cue cue){
  if(cue==null)return -1;
  switch(cue.kind){
   case NEUTRAL:case EXPLAIN:return 0;
   case LISTEN:return 42;
   case THINK:return 38;
   case JOY:return 1;
   case CONFIDENT:return 53;
   case ANGER:return 15;
   case SAD:return 10;
   case CRY:return cue.intensity>=3?28:cue.intensity>=2?21:35;
   case SURPRISE:return cue.intensity>=3?46:cue.intensity>=2?45:44;
   case SHY:return 8;
   case WORRY:return 43;
   case REFUSE:return 55;
   case TIRED:return 9;
   case RECOVERY:return 52;
   case STRAIN:return cue.intensity>=3?49:47;
   default:return -1;
  }
 }
}
