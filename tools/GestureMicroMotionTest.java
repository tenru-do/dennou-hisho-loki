package com.example.rokidkeyboardbridge;
import java.util.Arrays;
public final class GestureMicroMotionTest {
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static float[] grid(){float[] a=new float[17*33*2];for(int r=0;r<=32;r++)for(int c=0;c<=16;c++){int i=(r*17+c)*2;a[i]=240*c/16f;a[i+1]=600*r/32f;}return a;}
 public static void main(String[] args){
  float[] original=grid(),a=grid();GestureMicroMotion.apply(a,16,32,240,600,120,20,590,1100,0);ok(Arrays.equals(original,a),"body off means identical image mesh");
  float[] b=grid();GestureMicroMotion.apply(a,16,32,240,600,120,20,590,1100,1);GestureMicroMotion.apply(b,16,32,240,600,120,20,590,3300,1);ok(!Arrays.equals(a,b),"breathing varies within the same keyframe");
  int moving=0;
  for(int i=0;i<a.length;i+=2){float x=original[i],y=original[i+1],v=(y-20)/570f;
   ok(Float.isFinite(a[i])&&Float.isFinite(a[i+1]),"finite mesh");
   ok(Math.abs(a[i]-x)<.7f&&Math.abs(a[i+1]-y)<1.9f,"bounded displacement");
   if(v<=.17f||v>=.48f||x<=52.8f||x>=187.2f)ok(a[i]==x&&a[i+1]==y,"head hands legs edges and floor anchored");
   else if(a[i]!=x||a[i+1]!=y)moving++;
  }
  ok(moving>0,"localized breathing is visible");
  float[] again=grid();GestureMicroMotion.apply(again,16,32,240,600,120,20,590,5500,1);ok(Arrays.equals(a,again),"continuous periodic motion");
  float[] negative=grid();GestureMicroMotion.apply(negative,16,32,240,600,120,20,590,1100,-1);ok(Arrays.equals(negative,original),"disabled motion");
  float[] max=grid(),cap=grid();GestureMicroMotion.apply(max,16,32,240,600,120,20,590,1100,100);GestureMicroMotion.apply(cap,16,32,240,600,120,20,590,1100,1.25f);ok(Arrays.equals(max,cap),"gain capped");
  System.out.println("GestureMicroMotionTest passed "+checks+" checks");
 }
}
