package com.example.rokidkeyboardbridge;
/** Conversation variations preserve semantic family; explicit requests keep their chosen motion. */
final class CharacterGestureVariation {
 private int previous=-1,sequence;private long at=-1;
 void clear(){previous=-1;at=-1;sequence=0;}
 int choose(int family,long now,boolean explicit){
  if(explicit||family<0)return family;
  int next=family;
  if(at>=0&&now>=at&&now-at<18000&&family==previous){
   // Avoid immediate ear-touch / hand-waving repeats without substituting an unrelated pose.
   next=-1;
  }
  if(next>=0){previous=next;at=now;sequence++;}return next;
 }
 int variant(){return Math.floorMod(sequence,3);}
 static float pace(int variant){return variant==1?.94f:variant==2?1.06f:1f;}
}
