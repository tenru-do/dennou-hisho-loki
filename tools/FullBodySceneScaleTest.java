package com.example.rokidkeyboardbridge;
public final class FullBodySceneScaleTest {
 static int checks;static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 public static void main(String[]args){
  for(int[] screen:new int[][]{{160,640},{240,601},{480,640},{240,320},{320,480}}){int w=screen[0],h=screen[1];float ref=CharacterViewport.eyeSpan(w,h);
   for(int g=0;g<13;g++)for(int i=365+g*6;i<365+(g+1)*6;i++){float[] f=FullBodyRealisticFeatures.ALL[i];float scale=FullBodyCyberMotion.scale(i,(int)f[0],(int)f[1],w,h);check(Math.abs(scale*CharacterDistanceCalibration.BODY[i]-ref)<.002,"same distance cyber "+i);}
   for(int g=0;g<8;g++)for(int i=0;i<FullBodyDramaticFeatures.ALL[g].length;i++){float[] f=FullBodyDramaticFeatures.ALL[g][i];float scale=FullBodySceneScalePolicy.dramatic(g,i,w,h),left=FullBodySceneScalePolicy.left(g,i,w,scale),top=FullBodySceneScalePolicy.top(g,i,h,scale);check(Math.abs(scale*CharacterDistanceCalibration.DRAMATIC[g][i]-ref)<.002,"same distance special");check(Math.abs(left+(f[2]+f[8])*.5f*scale-w*.5f)<.002,"face center");check(top+Math.max(0,FullBodySceneScalePolicy.BOUNDS[g][i][2]-2)*scale>=CharacterViewport.headMargin(h)-.002,"crown retained");}
   for(int i=0;i<FullBodyApprovedFeatures.ALL.length;i++){float[] f=FullBodyApprovedFeatures.ALL[i];float height=FullBodyApprovedScalePolicy.height(i,w,h,1);check(Math.abs(height/f[1]*CharacterDistanceCalibration.APPROVED[i]-ref)<.002,"same distance approved/profile");check(Float.isFinite(height)&&height>0,"finite profile size");}
  }
  check(CharacterViewport.eyeSpan(160,640)==CharacterViewport.eyeSpan(480,640),"narrow viewport crops instead of moving camera");
  System.out.println("PASS "+checks+" shared full-body distance, profile fallback and crown checks");
 }
}
