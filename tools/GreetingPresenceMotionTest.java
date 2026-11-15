package com.example.rokidkeyboardbridge;
public final class GreetingPresenceMotionTest{
 public static void main(String[] args){int count=0;float[] a=new float[17*33*2],b=new float[a.length];
  for(int h:new int[]{384,768,1536})for(int w:new int[]{256,512,1024}){
   GreetingPresenceMotion.fill(a,16,32,w,h,w*.51f,h*.02f,h*.98f,1400,1);
   GreetingPresenceMotion.fill(b,16,32,w,h,w*.51f,h*.02f,h*.98f,4200,1);int changed=0;
   for(int r=0;r<=32;r++)for(int c=0;c<=16;c++){int i=(r*17+c)*2;float x=w*c/16f,y=h*r/32f;
    if(a[i]!=x||b[i]!=x)throw new AssertionError("no lateral scale or camera drift");count++;
    if(Math.abs(a[i+1]-y)>h*.00181f)throw new AssertionError("bounded breath");count++;
    if(r/32f<.20f||r/32f>.38f){if(a[i+1]!=y||b[i+1]!=y)throw new AssertionError("face, hands and legs must stay fixed");count++;}
    if(a[i+1]!=b[i+1])changed++;
   }if(changed==0)throw new AssertionError("breathing not visible in mesh");count++;
   GreetingPresenceMotion.fill(a,16,32,w,h,w*.51f,h*.02f,h*.98f,1400,0);
   for(int r=0;r<=32;r++)for(int c=0;c<=16;c++){if(a[(r*17+c)*2+1]!=h*r/32f)throw new AssertionError("disabled motion respected");count++;}
  }System.out.println("PASS "+count+" chest-only breathing, fixed face/arms/legs, amplitude and disable checks");
 }
}
