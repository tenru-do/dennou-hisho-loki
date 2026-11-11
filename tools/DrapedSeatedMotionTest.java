package com.example.rokidkeyboardbridge;
public final class DrapedSeatedMotionTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){float[] v=new float[17*33*2],zero=new float[4];boolean moved=false;
 for(int pose=0;pose<3;pose++)for(long t=0;t<26000;t+=251){SeatedPresenceMotion.fill(v,16,32,1024,1536,.16f,zero);DrapedSeatedMotion.apply(v,16,32,1024,1536,pose,t,1);
 for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){float x=col/16f*1024,y=row/32f*1536;int a=(row*17+col)*2;ok(Math.abs(v[a]-x)<13&&Math.abs(v[a+1]-y)<9,"small local leg motion");if(row/32f<.4f||row==32||col==0||col==16)ok(v[a]==x&&v[a+1]==y,"head hand torso and border retained");if(v[a]!=x||v[a+1]!=y)moved=true;}}
 ok(moved,"long drape actually animates");System.out.println("PASS "+n+" approved draped ankle and cloth, fixed head/torso and local displacement checks");}
}