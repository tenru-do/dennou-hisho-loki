package com.example.rokidkeyboardbridge;

/** Neutral-frame source eye units; expression/turning frames retain one scale. */
final class GestureViewportGeometry {
 static float eyeSpan(int clip,int imageWidth,int imageHeight,float referenceStature){
  if(StandingGestureAtlas.supports(clip))return clip==2||clip==24?18.028f*imageHeight/512f:15.508f*imageHeight/362f;
  if(clip==3||clip==7)return 28.443f*imageWidth/627f;
  if(SeatedGestureAtlas.supports(clip))return 14.036f*imageWidth/256f;
  if(clip==28||clip==29)return 19.925f*imageWidth/384f;
  if(clip==26)return 26.926f*imageWidth/384f;
  if(clip==27)return 27.295f*imageWidth/384f;
  if(clip==30)return 23.345f*imageWidth/384f;
  if(clip==31)return 23.195f*imageWidth/384f;
  if(clip==32||clip==33)return 35.511f*imageWidth/512f;
  if(clip==34||clip==35)return 31.016f*imageWidth/256f;
  // Older motion families already carry a standing-equivalent stature. Keep
  // that anatomical calibration until a reviewed neutral-eye unit is available.
  return Math.max(1,referenceStature)*26.849918f/719f;
 }
 static float centerOffset(int clip,int imageHeight){
  if(StandingGestureAtlas.supports(clip))return clip==2||clip==24?5f*imageHeight/512f:4.25f*imageHeight/362f;
  return 0;
 }
 private GestureViewportGeometry(){}
}
