package com.example.rokidkeyboardbridge;
/** Approved ordinary sitting/resting artwork only. No change to the conversation prompt. */
final class ApprovedQuietPosePolicy {
 static final String[] ASSETS={"loki_approved_seated_thought_20261102.png","loki_approved_seated_cross_a_20261102.png","loki_approved_seated_cross_b_20261102.png","loki_approved_sleep_20261102.png","loki_approved_rest_arms_20261102.png","loki_approved_rest_hands_20261102.png","loki_approved_rest_listening_20261102.png","loki_approved_sofa_relaxed_20261102.png"};
 static boolean seatedIndex(int index){return valid(index)&&(index<3||index==7);}
 static boolean sofa(String text){return VisualConversationPlan.clean(text).replaceAll("[。！!\\s]+$", "").matches("(?:ソファ(?:ー)?(?:で|に)(?:くつろいで|座って|腰かけて)|ソファ(?:ー)?の姿にして)");}
 // Ordinary sitting starts at one stable image; the separate leg timeline owns changes.
 static int seated(int mode,boolean thinking,boolean natural,long age){return 0;}
 static int resting(boolean natural,long age){return 3+(natural?(int)(Math.max(0,age)/60000%2):0);}
 static boolean awake(int index){return index<3||index==6||index==7;}
 static boolean valid(int index){return index>=0&&index<ASSETS.length;}
 static float[] fit(int width,int height,int imageWidth,int imageHeight){
  if(width<=0||height<=0||imageWidth<=0||imageHeight<=0)throw new IllegalArgumentException("pose dimensions");
  float scale=Math.min(width*.96f/imageWidth,height*.96f/imageHeight)*1.10f,w=imageWidth*scale,h=imageHeight*scale;
  // Uniform enlargement; keep the entire source top visible even when feet crop.
  return new float[]{(width-w)/2,Math.max(height*.02f,(height-h)/2),w,h};
 }
 // Crown and lowest visible foot in the reviewed, transparent source assets.
 // Feet determine translation only; they must never make a seated figure smaller.
 static final float[][] SEATED_VERTICAL={{18,1521},{32,1534},{39,1504},null,null,null,null,{48,1703}};
 static float[] fit(int index,int width,int height,int imageWidth,int imageHeight){
  if(!valid(index))throw new IllegalArgumentException("pose index");
  float[] face=FACE[index];float crown=seatedIndex(index)?SEATED_VERTICAL[index][0]:0;
  return calibratedFit(width,height,face,CharacterDistanceCalibration.QUIET[index],crown);
 }
 static float[] calibratedFit(int width,int height,float[] face,float unit,float crown){
  return CharacterViewport.fit(width,height,face[0],face[1],(face[2]+face[8])*.5f,unit,crown);
 }

 static float[] seatedFit(int width,int height,int imageWidth,int imageHeight,float[] face,float crown,float foot){
  if(width<=0||height<=0||imageWidth<=0||imageHeight<=0)throw new IllegalArgumentException("pose dimensions");
  // Destination geometry stays in original source coordinates even when the
  // decoder rounds dimensions. The standing eye unit owns every seated route.
  return CharacterViewport.fit(width,height,face[0],face[1],(face[2]+face[8])*.5f,CharacterViewport.eyes(face),crown);
 }
 static float fade(long age){return Math.min(1,Math.max(0,age)/650f);}
 // Original pixel-space eyes and mouth landmarks: widths are half spans, not body scaling.
 static final float[][] FACE={
  {1024.00000000f,1536.00000000f,467.00000000f,181.00000000f,20.00000000f,8.00000000f,5.00000000f,1.00000000f,529.00000000f,190.00000000f,20.00000000f,8.00000000f,5.00000000f,1.00000000f,486.00000000f,242.00000000f,25.00000000f,8.00000000f,4.00000000f},
  {1024.00000000f,1536.00000000f,466.00000000f,172.00000000f,21.00000000f,10.00000000f,5.00000000f,1.00000000f,528.00000000f,186.00000000f,21.00000000f,10.00000000f,5.00000000f,1.00000000f,483.00000000f,237.00000000f,27.00000000f,10.00000000f,4.00000000f},
  {1024.00000000f,1536.00000000f,478.00000000f,170.00000000f,21.00000000f,10.00000000f,5.00000000f,1.00000000f,532.00000000f,182.00000000f,21.00000000f,10.00000000f,5.00000000f,1.00000000f,492.00000000f,228.00000000f,25.00000000f,10.00000000f,4.00000000f},
  {911.00000000f,1727.00000000f,431.00000000f,198.00000000f,17.00000000f,-20.00000000f,2.00000000f,0.00000000f,496.00000000f,171.00000000f,17.00000000f,-20.00000000f,2.00000000f,0.00000000f,480.00000000f,233.00000000f,20.00000000f,-17.00000000f,3.00000000f},
  {841.00000000f,1870.00000000f,441.00000000f,201.00000000f,22.00000000f,-12.00000000f,1.00000000f,0.00000000f,524.00000000f,184.00000000f,27.00000000f,-12.00000000f,1.00000000f,0.00000000f,490.00000000f,239.00000000f,30.00000000f,-12.00000000f,3.00000000f},
  {856.00000000f,1837.00000000f,377.00000000f,190.00000000f,20.00000000f,-17.00000000f,1.00000000f,0.00000000f,445.00000000f,172.00000000f,25.00000000f,-17.00000000f,1.00000000f,0.00000000f,420.00000000f,231.00000000f,29.00000000f,-17.00000000f,3.00000000f},
  {836.00000000f,1881.00000000f,370.00000000f,239.00000000f,20.00000000f,-30.00000000f,4.00000000f,1.00000000f,426.00000000f,210.00000000f,20.00000000f,-30.00000000f,4.00000000f,1.00000000f,411.00000000f,280.00000000f,27.00000000f,-30.00000000f,4.00000000f},
  {887.00000000f,1774.00000000f,342.00000000f,183.00000000f,18.50000000f,20.00000000f,5.00000000f,1.00000000f,405.00000000f,203.00000000f,19.50000000f,20.00000000f,5.00000000f,1.00000000f,360.00000000f,247.00000000f,24.00000000f,18.00000000f,4.00000000f}
 };
}
