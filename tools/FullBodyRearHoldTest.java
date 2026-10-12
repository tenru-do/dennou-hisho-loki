package com.example.rokidkeyboardbridge;
public final class FullBodyRearHoldTest{
 public static void main(String[]args){
  for(int width:new int[]{240,480})for(int height:new int[]{299,598,640}){float h=FullBodyRearHoldPolicy.bodyHeight(width,height);
   for(float[]b:FullBodyRearHoldPolicy.BOUNDS){float scale=h/(b[3]-b[2]);if((b[1]-b[0])*scale>width-2+.01||h>height+.01)throw new AssertionError("rear frame fit");}
  }
  for(long t=0;t<15600;t++){float mix=FullBodyRearHoldPolicy.blend(t);if(mix<0||mix>1||FullBodyRearHoldPolicy.from(t)<0||FullBodyRearHoldPolicy.to(t)>5)throw new AssertionError("loop range");
   if(FullBodyRearHoldPolicy.from(t)==FullBodyRearHoldPolicy.from(t+1)&&Math.abs(mix-FullBodyRearHoldPolicy.blend(t+1))>.004)throw new AssertionError("loop discontinuity");
  }
  System.out.println("PASS rear hold six frames, 36 viewport fit checks and 15600 smooth cyclic samples");
 }
}
