package com.example.rokidkeyboardbridge;

/** One manually reviewed source calibration shared by all four seated greeting frames. */
final class SeatedGreetingGeometry {
 // Source frame width/height; left/right eye centers use the quiet-pose landmark layout.
 // Unused eye-patch fields are zero: these landmarks calibrate geometry only.
 static final float[] FACE={512,768,251,84,0,0,0,0,280,86};
 static float[] fit(int width,int height,int bitmapWidth,int bitmapHeight){
  return ApprovedQuietPosePolicy.seatedFit(width,height,bitmapWidth,bitmapHeight,FACE,14,754);
 }
}
