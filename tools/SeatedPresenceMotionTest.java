package com.example.rokidkeyboardbridge;
public final class SeatedPresenceMotionTest {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){float[] motion=new float[4],v=new float[17*33*2];
 for(long t=0;t<40000;t+=137){SeatedPresenceMotion.sample(motion,t,1,true,QuietPosePerformance.THOUGHT,.5f);SeatedPresenceMotion.fill(v,16,32,362,543,.16f,motion);
 for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){int at=(row*17+col)*2;float x=col/16f,y=row/32f;ok(Math.abs(v[at]-x*362)<2.0f&&Math.abs(v[at+1]-y*543)<3,"bounded displacement");if(row==32||col==0||col==16)ok(v[at]==x*362&&v[at+1]==y*543,"foot and edges fixed");}
 ok(SeatedPresenceMotion.upper(.12f,.16f)==SeatedPresenceMotion.upper(.22f,.16f),"chin hand stays with face");}
 SeatedPresenceMotion.sample(motion,5500,0,true,1,1);for(float x:motion)ok(x==0,"quiet scene motion suppressed");
 for(int id:new int[]{168,181,182,183,184,185})for(int[] view:new int[][]{{480,640},{240,640}}){float[] f=FullBodyRealisticFeatures.ALL[id];float scale=FullBodyScalePolicy.scale(id,(int)f[0],(int)f[1],view[0],view[1]);ok(Math.abs(scale*CharacterViewport.eyes(f)-CharacterViewport.eyeSpan(view[0],view[1]))<.002f,"feet-together matches standing eye unit");float sampled=FullBodyScalePolicy.scale(id,(int)f[0]/2,(int)f[1]/2,view[0],view[1]);ok(Math.abs(sampled*((int)f[0]/2)-scale*f[0])<.002f,"decoder does not shrink anatomy");}
 System.out.println("PASS "+n+" seated scale, rigid chin-hand, bounded movement and fixed feet");}
}