package com.example.rokidkeyboardbridge;
public final class RestPresenceMotionTest {
 public static void main(String[] args){int changes=0;
  for(boolean seated:new boolean[]{false,true})for(long time:new long[]{0,700,1700,2800,5300,14700}){
   float[] mesh=new float[17*33*2];for(int y=0;y<=32;y++)for(int x=0;x<=16;x++){int i=(y*17+x)*2;mesh[i]=512f*x/16;mesh[i+1]=768f*y/32;}float[] before=mesh.clone();RestPresenceMotion.apply(mesh,16,32,512,768,.18f,time,1,seated);
   for(int y=0;y<=32;y++)for(int x=0;x<=16;x++){int i=(y*17+x)*2;if(Math.abs(mesh[i]-before[i])>4||Math.abs(mesh[i+1]-before[i+1])>4)throw new AssertionError("unbounded motion");if(y<7||x==0||x==16||y==32){if(mesh[i]!=before[i]||mesh[i+1]!=before[i+1])throw new AssertionError("face/edges must stay fixed");}if(mesh[i]!=before[i]||mesh[i+1]!=before[i+1])changes++;}
   float[] frozen=before.clone();RestPresenceMotion.apply(frozen,16,32,512,768,.18f,time,0,seated);for(int i=0;i<frozen.length;i++)if(frozen[i]!=before[i])throw new AssertionError("movement off ignored");
  }if(changes<100)throw new AssertionError("no visible source motion");System.out.println("PASS ordinary rest breathing/settling, rigid head, disabled setting and bounds: "+changes);
 }
}
