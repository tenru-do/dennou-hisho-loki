package com.example.rokidkeyboardbridge;
/** Conversation variations preserve semantic family; explicit requests keep their chosen motion. */
final class CharacterGestureVariation {
 private final int[] recent={-1,-1,-1};private final long[] recentAt=new long[3];private int cursor;
 private int previous=-1,sequence;private long at=-1;
 void clear(){java.util.Arrays.fill(recent,-1);cursor=0;previous=-1;at=-1;sequence=0;}
 int choose(int family,long now,boolean explicit){
  if(explicit||family<0)return family;
  if(!ConversationStageMotion.optional(now,false))return -1;
  for(int i=0;i<recent.length;i++)if(recent[i]==family&&now>=recentAt[i]&&now-recentAt[i]<cooldown())return -1;
  int next=family;
  if(at>=0&&now>=at&&now-at<cooldown()&&family==previous){
   // Avoid immediate ear-touch / hand-waving repeats without substituting an unrelated pose.
   next=-1;
  }
  if(next>=0){previous=next;at=now;sequence++;recent[cursor]=next;recentAt[cursor]=now;cursor=(cursor+1)%recent.length;}return next;
 }
 static long cooldown(){return CharacterMotionTuning.frequency==0?45000:CharacterMotionTuning.frequency==2?9000:18000;}
 int variant(){return Math.floorMod(sequence,3);}
 static float pace(int variant){return variant==1?.94f:variant==2?1.06f:1f;}
}
