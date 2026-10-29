package com.example.rokidkeyboardbridge;
/** A completed directional swipe; never consumes or forwards an app command. */
final class CharacterSwipePolicy {
 static int direction(float dx,float dy,float minimum,long duration){
  if(duration<0||duration>2000||Math.max(Math.abs(dx),Math.abs(dy))<minimum)return 0;
  return Math.abs(dx)>=Math.abs(dy)?(dx>0?1:2):(dy>0?3:4);
 }
 static boolean feedback(boolean contact,boolean waiting){return contact||waiting;}
 static boolean contact(String text,long now,long contextAt){
  return contextAt>0&&now>=contextAt&&now-contextAt<180000&&ButtonReactionContext.touching(text);
 }
}
