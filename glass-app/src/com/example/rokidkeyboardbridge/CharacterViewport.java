package com.example.rokidkeyboardbridge;
/** Anatomical scale first. Crop overflow; never fit each pose's bounding box to the screen. */
final class CharacterViewport {
 // Reviewed ordinary standing pose 4. These are anatomical landmarks, not
 // an individual pose's canvas/bounding-box dimensions.
 private static final float STANDING_WIDTH=206,STANDING_HEIGHT=719;
 private static final float EYE_SPAN=(float)Math.hypot(123.675f-96.842f,71.394f-70.441f);
 // Restore the ordinary v742 camera, not the enlarged pose-436 outlier.
 // Width changes crop the view, never move the person away from the viewer.
 static final float MAXIMUM_EYE_AT_640=CharacterDistanceCalibration.REFERENCE_EYE_AT_640;
 static float eyeSpan(int width,int height){return MAXIMUM_EYE_AT_640*Math.max(1,height)/640f;}
 static float eyeScale(float sourceSpan,int width,int height){return eyeSpan(width,height)/Math.max(.001f,sourceSpan);}
 static float eyes(float[] face){return (float)Math.hypot(face[8]-face[2],face[9]-face[3]);}
 static float left(int width,float eyeCenter,float scale){return width*.5f-eyeCenter*scale;}
 static float standingHead(int width,int height){return eyeSpan(width,height)*38.59536f/EYE_SPAN;}
 static float stature(int width,int height){return eyeSpan(width,height)*STANDING_HEIGHT/EYE_SPAN;}
 static float scale(float referenceStature,int width,int height){return stature(width,height)/Math.max(1,referenceStature);}
 static float calibratedHead(float sourceHead,int width,int height){return standingHead(width,height)/Math.max(1,sourceHead);}
 static float headMargin(int height){return Math.max(6,Math.min(12,height*.02f));}
 /** Uniform anatomy scale. Wide limbs/feet may crop; the face stays centered. */
 static float[] fit(int width,int height,float sourceWidth,float sourceHeight,
                    float eyeCenter,float sourceEyeSpan,float crown){
  if(width<=0||height<=0||sourceWidth<=0||sourceHeight<=0||sourceEyeSpan<=0)
   throw new IllegalArgumentException("character geometry");
  float scale=eyeScale(sourceEyeSpan,width,height);
  return new float[]{left(width,eyeCenter,scale),headMargin(height)-Math.max(0,crown)*scale,sourceWidth*scale,sourceHeight*scale};
 }
 /** Translation only: never reduce the anatomical scale to preserve the feet. */
 static float top(float desired,float crown,float scale,int height){return Math.max(desired,headMargin(height)-Math.max(0,crown)*scale);}
 private CharacterViewport(){}
}
