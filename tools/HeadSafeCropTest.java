package com.example.rokidkeyboardbridge;
public final class HeadSafeCropTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){int oldClipped=0,clasped=0;int[][] views={{240,600},{240,320},{320,640},{360,720},{120,300}};
 for(int[] v:views)for(int pose=0;pose<FullBodyRealisticFeatures.ALL.length;pose++){
  float[] f=FullBodyRealisticFeatures.ALL[pose];float scale=FullBodyScalePolicy.scale(pose,(int)f[0],(int)f[1],v[0],v[1]);float crown=Math.max(0,FullBodyFrameScalePolicy.BOUNDS[pose][2]-2);float old=v[1]-f[1]*scale;float top=CharacterViewport.top(old,crown,scale,v[1]);
  ok(Float.isFinite(top)&&Float.isFinite(scale)&&scale>0,"finite placement "+pose);ok(top+crown*scale>=CharacterViewport.headMargin(v[1])-.001f,"crown remains inside "+pose);ok(top>=old,"only translate down; never enlarge or shrink");if(old+crown*scale<CharacterViewport.headMargin(v[1])){oldClipped++;if(pose==8||pose==17||pose==22||pose==24||pose==25)clasped++;}else ok(top==old,"safe existing position stays unchanged");
 }
 ok(oldClipped>0&&clasped>0,"reproduces hands-clasped top overflow");
 for(float desired:new float[]{-800,-10,0,25,100})for(float crown:new float[]{0,5,25,100}){float fixed=CharacterViewport.top(desired,crown,1.2f,600);ok(fixed+crown*1.2f>=12-.001f,"translation invariant");}
 System.out.println("PASS "+n+" head-priority placement checks; corrected overflow cases="+oldClipped+", clasped="+clasped);
 }
}
