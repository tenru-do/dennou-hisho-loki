package com.example.rokidkeyboardbridge;
/** Explicit stillness wins; selecting an ordinary seated image does not reset its idle clock. */
final class FullBodyIdlePolicy {
 static boolean ordinarySeat(int picture){return picture>=0&&picture<3;}
 static boolean resetLeg(boolean resting,boolean seated,int picture,int preview,int dramatic,int motionPreview){
  return resting||!seated||picture>=0&&!ordinarySeat(picture)||preview>=0||dramatic>=0||motionPreview>=0;
 }
 private FullBodyIdlePolicy(){}
}
