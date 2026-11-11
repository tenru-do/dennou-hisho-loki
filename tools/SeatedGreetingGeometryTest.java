package com.example.rokidkeyboardbridge;

/** Pure anatomical-size regression for the quiet-seat to greeting transition. */
public final class SeatedGreetingGeometryTest {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void close(float actual,float expected,String message){
  check(Math.abs(actual-expected)<.001f,message+": "+actual+" != "+expected);
 }
 private static float eyeSpan(float[] box,float[] face){
  return (float)Math.hypot((face[8]-face[2])*box[2]/face[0],(face[9]-face[3])*box[3]/face[1]);
 }
 public static void main(String[] args){
  float[] face=SeatedGreetingGeometry.FACE;
  check(face.length==10,"Only fixed geometry landmarks are supplied");
  close(face[0],512,"Reviewed source frame width");close(face[1],768,"Reviewed source frame height");
  for(int[] view:new int[][]{{480,640},{240,601},{320,900},{900,320},{512,768}}){
   int width=view[0],height=view[1];
   float[] quiet=ApprovedQuietPosePolicy.fit(0,width,height,1024,1536);
   float[] normal=SeatedGreetingGeometry.fit(width,height,512,768);
   close(eyeSpan(normal,face),eyeSpan(quiet,ApprovedQuietPosePolicy.FACE[0]),"Same apparent eye span as approved seated reference");
   float scale=normal[2]/face[0];
   check(scale>0&&Float.isFinite(scale),"Positive finite anatomical scale");
   close(scale,normal[3]/face[1],"One uniform source-space scale");
   close(normal[0]+(face[2]+face[8])*.5f*scale,width*.5f,"Eye midpoint stays horizontally centered");
   check(normal[1]+14*scale>=CharacterViewport.headMargin(height)-.001f,"Crown remains below the head margin");
   for(int[] decoded:new int[][]{{512,768},{256,384},{171,256},{1024,1536}}){
    float[] sampled=SeatedGreetingGeometry.fit(width,height,decoded[0],decoded[1]);
    for(int i=0;i<4;i++)close(sampled[i],normal[i],"Decoded dimensions do not alter displayed geometry component "+i);
    close(eyeSpan(sampled,face),eyeSpan(quiet,ApprovedQuietPosePolicy.FACE[0]),"Decoder rounding retains the approved head unit");
   }
  }
  float[] portrait=SeatedGreetingGeometry.fit(480,640,512,768);
  float scale=portrait[3]/768;
  close(eyeSpan(portrait,face),CharacterViewport.eyeSpan(480,640),"Standing distance owns greeting scale");
  float[] wide=CharacterViewport.fit(240,640,1400,768,700,CharacterViewport.eyes(face),14);
  check(wide[2]>240,"Wide source may crop instead of reducing the standing-distance unit");
  close(wide[0]+700*wide[2]/1400,120,"Wide source retains centered face");
  close(portrait[1]+14*scale,CharacterViewport.headMargin(640),"Head margin wins over floor alignment on overflow");
  System.out.println("PASS "+checks+" seated greeting/quiet eye-span, uniform geometry, decoder invariance and head-priority assertions");
 }
}
