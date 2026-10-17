package com.example.rokidkeyboardbridge;
public final class FullBodyFrameCoverageTest {
 public static void main(String[] args){int checks=0;
  for(int w:new int[]{240,480})for(int h:new int[]{299,601,640})for(int pose=0;pose<443;pose++){
   float[] f=FullBodyRealisticFeatures.ALL[pose],b=FullBodyFrameScalePolicy.BOUNDS[pose];
   for(int sample:new int[]{1,2}){int bw=Math.round(f[0]/sample),bh=Math.round(f[1]/sample);float scale=FullBodyScalePolicy.scale(pose,bw,bh,w,h),source=scale*bw/f[0];
    float left=FullBodyAnchorPolicy.left(pose,bw,w,scale,FullBodyMotionPolicy.bodyHeightFraction(pose));
    if(!Float.isFinite(scale)||scale<=0||left+b[0]*source<-.05f||left+b[1]*source>w+.05f||f[1]*source>h+1.5f)throw new AssertionError("Frame fit/decode "+pose+" "+w+"x"+h+" sample="+sample+" left="+left+" min="+(left+b[0]*source)+" max="+(left+b[1]*source)+" height="+(f[1]*source));
    checks++;
   }
  }
  for(int first=293;first<365;first+=6)for(int i=1;i<6;i++)if(Math.abs(FullBodyFrameScalePolicy.source(first,240,601)-FullBodyFrameScalePolicy.source(first+i,240,601))>.00001f)throw new AssertionError("Phase scale pulse "+first);
  for(int pose=0;pose<443;pose++){float[]f=FullBodyFaceAnimator.features(pose);if(f==null||f.length!=19)throw new AssertionError("Face coverage "+pose);for(int at:new int[]{2,8,14})if(f[at+2]>0&&(f[at]<0||f[at]>=f[0]||f[at+1]<0||f[at+1]>=f[1]))throw new AssertionError("Face bounds "+pose);}
  if(FullBodyScalePolicy.scale(293,364,530,240,601)<.6f)throw new AssertionError("Kneeling scale regression");
  System.out.println("PASS "+checks+" all-frame bounds, sample invariance and stable phase scales; 443 face records");
 }
}
