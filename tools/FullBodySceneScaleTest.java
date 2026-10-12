package com.example.rokidkeyboardbridge;
public final class FullBodySceneScaleTest {
 static int checks;static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 public static void main(String[]args){
  for(int[] screen:new int[][]{{240,601},{480,640},{240,320},{320,480}}){int w=screen[0],h=screen[1];float ref=FullBodySceneScalePolicy.reference(w,h);
   for(int g=0;g<13;g++){int first=365+g*6;float[] f=FullBodyRealisticFeatures.ALL[first];float scale=FullBodyCyberMotion.scale(first,(int)f[0],(int)f[1],w,h);
    check(scale>0&&Float.isFinite(scale),"cyber scale");check(scale*FullBodySceneScalePolicy.CYBER[g]<=ref*1.001f,"no oversized cyber head");
    for(int i=first;i<first+6;i++){float[] x=FullBodyRealisticFeatures.ALL[i];float v=FullBodyCyberMotion.scale(i,(int)x[0],(int)x[1],w,h);check(Math.abs(v-scale)<.001,"same scale across six phases");check((FullBodyAnchorPolicy.MAX[i]-FullBodyAnchorPolicy.MIN[i])*scale<=w-5.9,"visible width fits");check(x[1]*scale<=h+.01,"floor/top fit");}
   }
   for(int g=0;g<8;g++){float scale=FullBodySceneScalePolicy.dramatic(g,w,h);check(scale*FullBodySceneScalePolicy.DRAMATIC[g]<=ref*1.001,"dramatic head bound");for(int i=0;i<FullBodySceneScalePolicy.BOUNDS[g].length;i++){float[] b=FullBodySceneScalePolicy.BOUNDS[g][i];float left=FullBodySceneScalePolicy.left(g,i,w,scale),top=FullBodySceneScalePolicy.top(g,i,h,scale);check(left+b[0]*scale>=2.99&&left+b[1]*scale<=w-2.99,"dramatic sides fit");check(top+b[2]*scale>=-.01&&Math.abs(top+b[3]*scale-h)<.01,"dramatic floor fit");}}
  }
  float reference=FullBodySceneScalePolicy.reference(240,601);
  for(int g=0;g<13;g++){int p=365+g*6;float[] f=FullBodyRealisticFeatures.ALL[p];float actual=FullBodyCyberMotion.scale(p,(int)f[0],(int)f[1],240,601)*FullBodySceneScalePolicy.CYBER[g];check(actual/reference>.74f,"cyber must not shrink to half-size "+g);}
  for(int g=0;g<8;g++)check(FullBodySceneScalePolicy.dramatic(g,240,601)*FullBodySceneScalePolicy.DRAMATIC[g]/reference>.79f,"legacy must not shrink to half-size "+g);
  System.out.println("PASS scene calibration "+checks+" shared face units, stable six-phase scale, content fit and floor checks");
 }
}
