package com.example.rokidkeyboardbridge;
public final class FullBodyDramaticBreathTest {
 public static void main(String[] args){
  int count=FullBodyDramaticGallery.ASSETS.length;
  if(count!=7||FullBodyDramaticGallery.BREATH_SHEETS.length!=count||FullBodyDramaticGallery.EXHALE_SHIFT_X.length!=count||FullBodyDramaticGallery.EXHALE_SHIFT_Y.length!=count)throw new AssertionError("scene/anchor catalog mismatch");
  for(int id=0;id<count;id++)if(!Float.isFinite(FullBodyDramaticGallery.EXHALE_SHIFT_X[id])||Math.abs(FullBodyDramaticGallery.EXHALE_SHIFT_X[id])>0.15f||Math.abs(FullBodyDramaticGallery.EXHALE_SHIFT_Y[id])>0.02f)throw new AssertionError("unsafe anchor offset");
  if(FullBodyDramaticBreathPolicy.exhaleWeight(-1)!=0f)throw new AssertionError("negative clock");
  if(FullBodyDramaticBreathPolicy.exhaleWeight(0)!=0f)throw new AssertionError("initial inhale");
  if(FullBodyDramaticBreathPolicy.exhaleWeight(450)!=1f)throw new AssertionError("exhale peak");
  if(FullBodyDramaticBreathPolicy.exhaleWeight(900)!=0f)throw new AssertionError("cycle seam");
  for(long t=0;t<900;t++){
   float v=FullBodyDramaticBreathPolicy.exhaleWeight(t);
   if(v<0||v>1||!Float.isFinite(v))throw new AssertionError("invalid alpha");
   if(Math.abs(v-FullBodyDramaticBreathPolicy.exhaleWeight(t+900))>0.00001f)throw new AssertionError("repeat");
   if(Math.abs(v-FullBodyDramaticBreathPolicy.exhaleWeight(t+1))>0.004f)throw new AssertionError("discontinuous motion");
  }
  System.out.println("PASS dramatic breathing: fast period, inhale/exhale endpoints, alpha bounds, continuity and cycle seam");
 }
}
