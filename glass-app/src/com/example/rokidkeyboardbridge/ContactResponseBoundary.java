package com.example.rokidkeyboardbridge;
/** Consume existing response semantics locally. Does not alter model input or start requests. */
final class ContactResponseBoundary {
 static boolean refused(String reply){FullBodySemanticReaction.Reaction semantic=FullBodySemanticReaction.find(reply);if(semantic!=null)return semantic.cue.kind==FullBodyPoseCatalog.Kind.REFUSE;if(reply==null||!reply.matches("(?s).*(お断り|やめて|やめます|触らないで|触れないで|嫌です|いやです|手を離します|手をほどきます|接触は控え|制止します).*"))return false;FullBodySpeechStance.Cue cue=FullBodySpeechStance.classify(reply);return cue!=null&&cue.kind==FullBodyPoseCatalog.Kind.REFUSE;}
}
