package com.example.rokidkeyboardbridge;
public final class FixedCharacterDistanceTest {
 public static void main(String[] args){
  if(Math.abs(CharacterViewport.eyeSpan(480,640)-26.2897668f)>.00001)throw new AssertionError("user-selected seated reference at 1.10 times v744");
  for(int h:new int[]{320,640,900})for(int w:new int[]{100,160,240,480,900}){
   if(Math.abs(CharacterViewport.eyeSpan(w,h)-26.2897668f*h/640)>.0001)throw new AssertionError("camera scale must ignore lateral extent");
   float[] small=CharacterViewport.fit(w,h,300,900,150,30,12),wide=CharacterViewport.fit(w,h,1600,900,800,30,12);
   if(Math.abs(small[2]/300-wide[2]/1600)>.0001)throw new AssertionError("wide pose shrunk");
  }
  // Independent historical fixtures: these were measured before v743.
  float[] ref=ApprovedQuietPosePolicy.fit(0,480,640,1024,1536);
  if(Math.abs(ref[2]/1024f-.4196303616f)>.00001f)throw new AssertionError("approved seated source must be exactly 1.10 times v744");
  float[] f=FullBodyRealisticFeatures.ALL[436];float scale=FullBodyScalePolicy.scale(436,(int)f[0],(int)f[1],480,640);
  if(scale>=1.22169f)throw new AssertionError("outlier 436 cannot enlarge an already large image");
  float leg=CharacterViewport.eyeScale(CharacterDistanceCalibration.LEG,480,640);
  if(leg>=1.189059f)throw new AssertionError("already large v742 leg family must not grow");
  if(Math.abs(FullBodyScalePolicy.scale(4,206,719,480,640)/.90245104f-1.10f)>.0001)throw new AssertionError("standing must share the selected seated distance, not an independent multiplier");
  FullBodyFrameCoverageTest.main(args);FullBodySceneScaleTest.main(args);
  System.out.println("PASS reviewed historical baseline and same-person/same-distance invariants");
 }
}
