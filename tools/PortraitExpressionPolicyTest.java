package com.example.rokidkeyboardbridge;
public final class PortraitExpressionPolicyTest {
 static int n;static void check(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  boolean[] reachable=new boolean[8];
  for(long t=0;t<64000;t+=125)for(int mode=0;mode<=2;mode++){
   for(int expression=0;expression<62;expression++){
    int cell=PortraitExpressionPolicy.cell(expression,mode,t,-1,0,false);
    if(PortraitExpressionPolicy.supports(expression))check(cell>=0&&cell<8,"registered cell");else check(cell==-1,"semantic emotion retained");
    if(expression==0&&mode==0)reachable[cell]=true;
   }
   check(PortraitExpressionPolicy.cell(0,mode,t,0,7,true)==(mode==1?1:0),"navigation/recording stays quiet");
   check(PortraitExpressionPolicy.delay(mode,false)>=80&&PortraitExpressionPolicy.delay(mode,true)>=125,"bounded cadence");
  }
  for(int c=0;c<8;c++){check(PortraitExpressionPolicy.cell(0,0,1100,1000,c,false)==c,"all eight operation poses reachable");reachable[c]=true;}
  for(boolean b:reachable)check(b,"eight variants registered");
  check(PortraitExpressionPolicy.cell(0,0,2600,1000,7,false)!=7,"contact cue expires");
  check(PortraitExpressionPolicy.cell(0,0,999,1000,7,false)!=7,"future cue not used");
  check(PortraitExpressionPolicy.cell(6,0,1100,1000,7,false)==-1,"worry overrides contact smile");
  System.out.println("PASS "+n+" portrait selection/cue expiry/semantic precedence/cadence assertions");
 }
}
