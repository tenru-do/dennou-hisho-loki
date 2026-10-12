package com.example.rokidkeyboardbridge;
public final class FullBodyContinuousMotionTest{
 public static void main(String[]args){int checks=0;float[]v=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
  for(float[]f:FullBodyApprovedFeatures.ALL){float bottom=(f[15]+Math.max(f[16]*.25f,f[18])+4)/f[1];float previous=0;boolean moved=false;
   for(long t=0;t<=15600;t+=20){FullBodyBreathPolicy.resting(v,240,598,t,bottom);float y=v[(10*(FullBodyBreathPolicy.COLUMNS+1)+2)*2+1];moved|=Math.abs(y-598*10f/FullBodyBreathPolicy.ROWS)>.01;
    for(int row:new int[]{0,FullBodyBreathPolicy.ROWS})for(int col=0;col<=FullBodyBreathPolicy.COLUMNS;col++){int at=(row*(FullBodyBreathPolicy.COLUMNS+1)+col)*2;if(v[at]!=240*col/(float)FullBodyBreathPolicy.COLUMNS||v[at+1]!=598*row/(float)FullBodyBreathPolicy.ROWS)throw new AssertionError("Head/floor drift");}
    if(t>0&&Math.abs(y-previous)>.2f)throw new AssertionError("Breathing jump");previous=y;checks++;
   }if(!moved)throw new AssertionError("Frozen approved pose");
  }
  for(int id=259;id<=263;id++)if(FullBodyRealisticAssets.topStrayRows(id)<=0)throw new AssertionError("Foreign sheet fragment not excluded");
  System.out.println("PASS "+checks+" continuous approved motion samples over three breathing cycles, stationary head/floor and five crop masks");
 }
}
