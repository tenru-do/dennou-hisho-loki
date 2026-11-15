package com.example.rokidkeyboardbridge;
public final class ComfortPresenceMotionTest{
 public static void main(String[] args){int checks=0;
  for(int pose=0;pose<9;pose++){
   float[] a=new float[17*25*2],b=new float[a.length];
   for(int r=0;r<=24;r++)for(int c=0;c<=16;c++){int i=(r*17+c)*2;a[i]=b[i]=512f*c/16;a[i+1]=b[i+1]=768f*r/24;}
   ComfortPoseGeometry.presence(a,16,24,pose,12600,1);ComfortPoseGeometry.presence(b,16,24,pose,15400,1);int changed=0;
   for(int r=0;r<=24;r++)for(int c=0;c<=16;c++){int i=(r*17+c)*2;float y=768f*r/24;
    if(a[i]!=512f*c/16||b[i]!=512f*c/16)throw new AssertionError("horizontal/body-scale drift");checks++;
    if(Math.abs(a[i+1]-y)>1.16f)throw new AssertionError("breath too large");checks++;
    if(y<=ComfortPoseGeometry.FACE[pose][1]+ComfortPoseGeometry.FACE[pose][3]+20&&a[i+1]!=y)throw new AssertionError("head moved");checks++;
    if(a[i+1]!=b[i+1])changed++;
   }if(changed==0)throw new AssertionError("rest stopped after entry: "+pose);checks++;
  }System.out.println("PASS "+checks+" nine resting poses, late breathing, fixed face and uniform distance checks");
 }
}
