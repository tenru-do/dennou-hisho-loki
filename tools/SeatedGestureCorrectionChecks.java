package com.example.rokidkeyboardbridge;
public final class SeatedGestureCorrectionChecks {
 public static int run(){int n=0;for(int clip=0;clip<22;clip++){boolean corrected=clip%2==1;String asset=FullBodyGestureRenderer.assetForClip(clip);if(asset.equals(SeatedGestureAtlas.ASSET)!=corrected)throw new AssertionError("Dedicated seated artwork used by seated aliases only: "+clip);n++;}return n;}
}
