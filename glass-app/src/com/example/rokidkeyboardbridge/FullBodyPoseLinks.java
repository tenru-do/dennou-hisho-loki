package com.example.rokidkeyboardbridge;
/** Pose-specific six-frame links, bidirectional. Selection and posture timing stay separate. */
final class FullBodyPoseLinks {
 static final int[][] PAIRS={{4,2},{4,0},{168,165},{168,167},{4,1},{1,0}};
 static int canonical(int id){
  if(id>=169&&id<=174||id==13||id==31||id==37)return 4;
  if(id>=0&&id<168&&!FullBodyMotionPolicy.isSeated(id)){
   String gesture=FullBodyPoseCatalog.ALL[id].gesture;
   if(gesture.equals("palm_up"))return 0;
   if(gesture.equals("arms_crossed")||gesture.equals("crossed_arms"))return 2;
  }
  if(id==135)return 167;
  return id;
 }
 static int index(int from,int to){
  int a=canonical(from),b=canonical(to);
  for(int i=0;i<PAIRS.length;i++){
   if(i>=4&&(from==13||to==13))continue; // This alias already has a hand at its chin.
   if(a==PAIRS[i][0]&&b==PAIRS[i][1]||a==PAIRS[i][1]&&b==PAIRS[i][0])return i;
  }
  return -1;
 }
 static boolean reverse(int link,int from){return canonical(from)==PAIRS[link][1];}
 static int firstFrame(int link){return link>=4?305+(link-4)*6:198+link*6;}
 static int[] frames(int link){
  if(link==0)return new int[]{198,335,199,336,200,337,201,338,202,339,203};
  if(link==4)return new int[]{317,305,318,306,320,319,307,308,321,309,322,310};
  if(link==5)return new int[]{311,323,312,324,313,325,326,314,327,315,328,316};
  int first=firstFrame(link);return new int[]{first,first+1,first+2,first+3,first+4,first+5};
 }
}
