package com.example.rokidkeyboardbridge;
/** Anatomical scale first. Crop overflow; never fit each pose's bounding box to the screen. */
final class CharacterViewport {
 static float standingHead(int width,int height){return stature(width,height)/8.1f;}
 static float stature(int width,int height){return Math.min(width*2.35f,height*.94f);}
 static float scale(float referenceStature,int width,int height){return stature(width,height)/Math.max(1,referenceStature);}
 static float calibratedHead(float sourceHead,int width,int height){return standingHead(width,height)/Math.max(1,sourceHead);}
 static float headMargin(int height){return Math.max(6,Math.min(12,height*.02f));}
 /** Translation only: never reduce the anatomical scale to preserve the feet. */
 static float top(float desired,float crown,float scale,int height){return Math.max(desired,headMargin(height)-Math.max(0,crown)*scale);}
 private CharacterViewport(){}
}
