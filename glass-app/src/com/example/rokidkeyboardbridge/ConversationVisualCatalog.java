package com.example.rokidkeyboardbridge;
/** Existing artwork families: semantic reaction is independent of standing/seated/resting posture. */
final class ConversationVisualCatalog {
 static boolean priority(FullBodyPoseCatalog.Kind k){return k==FullBodyPoseCatalog.Kind.REFUSE||k==FullBodyPoseCatalog.Kind.WORRY||k==FullBodyPoseCatalog.Kind.SURPRISE;}
 static int restReaction(FullBodyPoseCatalog.Kind k){if(k==null)return RestConversationReaction.NONE;switch(k){
 case JOY:case CONFIDENT:return RestConversationReaction.HAPPY;
 case THINK:case EXPLAIN:return RestConversationReaction.THINK;
 case SURPRISE:return RestConversationReaction.SURPRISE;
 case WORRY:case SAD:case CRY:case TIRED:case REFUSE:case STRAIN:case ANGER:return RestConversationReaction.CARE;
 case SHY:return RestConversationReaction.TOUCH;
 default:return RestConversationReaction.ACK;
 }}
}
