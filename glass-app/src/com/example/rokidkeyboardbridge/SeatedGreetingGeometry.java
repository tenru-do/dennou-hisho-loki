package com.example.rokidkeyboardbridge;

/** One manually reviewed source calibration shared by all four seated greeting frames. */
final class SeatedGreetingGeometry {
 // Source frame width/height; left/right eye centers use the quiet-pose landmark layout.
 // Unused eye-patch fields are zero: these landmarks calibrate geometry only.
 static final float[] FACE={512,768,244.172195f,84,0,0,0,0,274.172195f,86};
 static float[] fit(int width,int height,int bitmapWidth,int bitmapHeight){
  return ApprovedQuietPosePolicy.calibratedFit(width,height,FACE,CharacterDistanceCalibration.GESTURE[1][0],14);
 }
}
