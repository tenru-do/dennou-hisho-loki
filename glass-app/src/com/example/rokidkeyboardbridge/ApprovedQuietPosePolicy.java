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
  if(seatedIndex(index))return seatedFit(width,height,imageWidth,imageHeight,FACE[index],SEATED_VERTICAL[index][0],SEATED_VERTICAL[index][1]);
  float[] face=FACE[index];
  // The top of the transparent source is a conservative crown limit. Never
  // shrink a reclining figure just because its arms extend across the canvas.
  return CharacterViewport.fit(width,height,face[0],face[1],(face[2]+face[8])*.5f,CharacterViewport.eyes(face),0);
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
  {1024,1536,467,181,20,8,5,1,529,190,20,8,5,1,486,242,25,8,4},
  {1024,1536,466,172,21,10,5,1,528,186,21,10,5,1,483,237,27,10,4},
  {1024,1536,478,170,21,10,5,1,532,182,21,10,5,1,492,228,25,10,4},
  {911,1727,431,198,17,-20,2,0,496,171,17,-20,2,0,480,233,20,-17,3},
  {841,1870,441,201,22,-12,1,0,524,184,27,-12,1,0,490,239,30,-12,3},
  {856,1837,377,190,20,-17,1,0,445,172,25,-17,1,0,420,231,29,-17,3},
  {836,1881,370,239,20,-30,4,1,426,210,20,-30,4,1,411,280,27,-30,4},
  {887,1774,342,183,18.5f,20,5,1,405,203,19.5f,20,5,1,360,247,24,18,4}
 };
}
