package com.example.rokidkeyboardbridge;
public final class FullBodyNeckTest {
 public static void main(String[] args){int checked=0;
 for(int pose=0;pose<168;pose++){if(!FullBodyMotionPolicy.isSeated(pose))continue;float[] f=FullBodyRealisticFeatures.ALL[pose];float face=(f[15]+Math.max(f[16]*.25f,f[18])+4)/f[1];
 for(long t=0;t<19400;t+=50){for(boolean laugh:new boolean[]{false,true}){float[] v=new float[17*33*2];if(laugh)FullBodyBreathPolicy.laugh(v,240,601,t,face);else FullBodyBreathPolicy.resting(v,240,601,t,face);FullBodyLivingPosePolicy.apply(v,240,601,t,face,FullBodyLivingPosePolicy.profile(pose,f));
 for(int row=0;row<=32;row++){if(row/32f>face+.045f)continue;for(int col=0;col<=16;col++){int a=(row*17+col)*2;if(Math.abs(v[a]-240*col/16f)>.001f||Math.abs(v[a+1]-601*row/32f)>.001f)throw new AssertionError("Neck deformation pose="+pose+" laugh="+laugh+" time="+t);checked++;}}}
 }}System.out.println("PASS "+checked+" seated head/neck vertices across resting/laugh cycles");}
}
