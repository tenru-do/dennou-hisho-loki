package com.example.rokidkeyboardbridge;
/** Wardrobe keyframes may move locally but must never fold or reverse a mesh cell. */
public final class SeatedLegWardrobeMeshTest {
 static int checks;static void positive(float ax,float ay,float bx,float by){checks++;if(ax*by-ay*bx<=0)throw new AssertionError("folded seated leg mesh");}
 public static void main(String[] args){
  for(int a=0;a<SeatedLegWardrobe.JOINTS.length;a++)for(int b=0;b<SeatedLegWardrobe.JOINTS.length;b++)if(Math.abs(a-b)==1)for(int t=0;t<=100;t++){
   float[] m=new float[17*33*2];for(int y=0;y<=32;y++)for(int x=0;x<=16;x++){int i=(y*17+x)*2;m[i]=x*362f/16;m[i+1]=y*543f/32;}
   SeatedLegTween.apply(m,16,32,362,543,a,b,t/100f,SeatedLegWardrobe.JOINTS);
   for(int y=0;y<32;y++)for(int x=0;x<16;x++){int i=(y*17+x)*2;positive(m[i+2]-m[i],m[i+3]-m[i+1],m[i+34]-m[i],m[i+35]-m[i+1]);positive(m[i+34]-m[i+36],m[i+35]-m[i+37],m[i+2]-m[i+36],m[i+3]-m[i+37]);}
  }
  System.out.println("PASS "+checks+" wardrobe mesh orientation checks");
 }
}
