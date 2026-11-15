package com.example.rokidkeyboardbridge;
/** Fixed-distance coverage: overflows crop; decoder size must not change anatomy. */
public final class FullBodyFrameCoverageTest {
 public static void main(String[] args){int checks=0;
  for(int w:new int[]{160,240,480})for(int h:new int[]{299,601,640})for(int pose=0;pose<443;pose++){
   float[] f=FullBodyRealisticFeatures.ALL[pose];
   for(int sample:new int[]{1,2,3}){int bw=Math.round(f[0]/sample),bh=Math.round(f[1]/sample);float scale=FullBodyScalePolicy.scale(pose,bw,bh,w,h),source=scale*bw/f[0];
    if(!Float.isFinite(scale)||scale<=0||Math.abs(source*CharacterDistanceCalibration.BODY[pose]-CharacterViewport.eyeSpan(w,h))>.001)throw new AssertionError("distance/decode "+pose);
    float center=(f[2]+f[8])*.5f,left=CharacterViewport.left(w,center,source);if(Math.abs(left+center*source-w*.5f)>.001)throw new AssertionError("face center "+pose);checks++;
   }
  }
  for(int pose=0;pose<443;pose++){float[]f=FullBodyRealisticFeatures.ALL[pose];if(f==null||f.length!=19)throw new AssertionError("Face coverage "+pose);for(int at:new int[]{2,8,14})if(f[at+2]>0&&(f[at]<0||f[at]>=f[0]||f[at+1]<0||f[at+1]>=f[1]))throw new AssertionError("Face bounds "+pose);}
  System.out.println("PASS "+checks+" fixed-distance all-frame decode and center checks; 443 face records");
 }
}
