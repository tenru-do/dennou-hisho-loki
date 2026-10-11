package com.example.rokidkeyboardbridge;
public final class FullBodyMotionPolicyTest {
 public static void main(String[] args) {
  for(int id=0;id<168;id++) {
   if(FullBodyMotionPolicy.poseForState(id,1)!=4) throw new AssertionError("Standing listening");
   if(FullBodyMotionPolicy.poseForState(id,2)!=FullBodyExpressionMap.pose(id))throw new AssertionError("Emotion mapping "+id);
   for(int mode=0;mode<3;mode++) {
    int pose=FullBodyMotionPolicy.poseForState(id,mode);
    if(pose<0||pose>168) throw new AssertionError("Pose bounds");
    float height=FullBodyMotionPolicy.bodyHeightFraction(pose);
    if(height!=(FullBodyMotionPolicy.isSeated(pose)?.76f:1f)) throw new AssertionError("Body scale");
    if(FullBodyMotionPolicy.usesIdleFrames(id,mode)&&FullBodyMotionPolicy.isSeated(pose)) throw new AssertionError("Seated blink isolation");
   }
  }
  if(FullBodyMotionPolicy.poseForState(0,0)!=168 || FullBodyMotionPolicy.poseForState(0,2)!=0 || FullBodyMotionPolicy.poseForState(7,2)!=7) throw new AssertionError("Rest/listen/reply transitions");
  if(FullBodyMotionPolicy.idleFrame(4620)!=2||FullBodyMotionPolicy.idleFrame(6000)!=0)throw new AssertionError("Blink boundaries");
  if(FullBodyMotionPolicy.poseForState(10,2)!=3 || FullBodyMotionPolicy.poseForState(15,2)!=49 || FullBodyMotionPolicy.poseForState(6,2)!=36 || FullBodyMotionPolicy.poseForState(2,2)!=10 || FullBodyMotionPolicy.poseForState(11,2)!=11) throw new AssertionError("Sadness, anger, apology, search, surprise artwork");
  System.out.println("PASS 168 expressions: automatic posture, common body proportions, seated idle isolation");
 }
}
