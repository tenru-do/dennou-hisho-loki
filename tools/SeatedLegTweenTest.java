package com.example.rokidkeyboardbridge;
public final class SeatedLegTweenTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){float[] v=new float[17*33*2],zero=new float[4];
 for(int i=0;i<7;i++)for(int direction=0;direction<2;direction++)for(float amount:new float[]{0,.25f,.5f,.75f,1}){
  int a=direction==0?i:i+1,b=direction==0?i+1:i;SeatedPresenceMotion.fill(v,16,32,362,543,.16f,zero);SeatedLegTween.apply(v,16,32,362,543,a,b,amount);boolean changed=false;
  for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){int at=(row*17+col)*2;float x=col/16f*362,y=row/32f*543;ok(Math.abs(v[at]-x)<=30.001f&&Math.abs(v[at+1]-y)<=24.001f,"bounded articulation");if(y<=220||row==32||col==0||col==16)ok(v[at]==x&&v[at+1]==y,"head hand torso and edges fixed");if(v[at]!=x||v[at+1]!=y)changed=true;}
  ok(changed==(amount>0),"real intermediate geometry, exact endpoint retained");}
 SeatedLegMotionPolicy p=new SeatedLegMotionPolicy();p.request(0,true);p.update(0,true,false,false,false,false);p.update(350,true,false,false,false,false);ok(Math.abs(p.blend()-.5f)<.001,"continuous motion across whole first interval");
 System.out.println("PASS "+n+" bidirectional anatomical intermediate, endpoint, bounds and continuous timing checks");}
}