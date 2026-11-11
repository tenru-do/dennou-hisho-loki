package com.example.rokidkeyboardbridge;

/** Shared standing-distance and head-first cropping contracts, without Android. */
public final class CharacterViewportScaleTest {
 private static int checks;
 private static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 private static void close(float a,float b,String message){ok(Math.abs(a-b)<.002f,message+": "+a+" / "+b);}
 private static float span(float[] box,float[] face){return CharacterViewport.eyes(face)*box[2]/face[0];}
 public static void main(String[] args){
  for(int[] view:new int[][]{{480,640},{240,601},{320,900},{900,320},{160,900}}){
   int w=view[0],h=view[1];float unit=CharacterViewport.eyeSpan(w,h);
   float[] standing=FullBodyRealisticFeatures.ALL[4];
   close(unit,CharacterViewport.eyes(standing)*FullBodyScalePolicy.scale(4,(int)standing[0],(int)standing[1],w,h),"Existing ordinary standing size retained");
   for(int i=0;i<ApprovedQuietPosePolicy.ASSETS.length;i++){
    float[] face=ApprovedQuietPosePolicy.FACE[i],box=ApprovedQuietPosePolicy.fit(i,w,h,(int)face[0],(int)face[1]);
    close(span(box,face),unit,"Same seated/resting eye unit "+i);
    float scale=box[2]/face[0];close(scale,box[3]/face[1],"Uniform XY scale "+i);
    close(box[0]+(face[2]+face[8])*.5f*scale,w*.5f,"Face centered "+i);
    float crown=ApprovedQuietPosePolicy.seatedIndex(i)?ApprovedQuietPosePolicy.SEATED_VERTICAL[i][0]:0;
    close(box[1]+crown*scale,CharacterViewport.headMargin(h),"Crown priority "+i);
    float[] sampled=ApprovedQuietPosePolicy.fit(i,w,h,(int)(face[0]/3),(int)(face[1]/3));
    for(int k=0;k<4;k++)close(box[k],sampled[k],"Decoder rounding cannot change person scale "+i);
   }
   float[] greeting=SeatedGreetingGeometry.fit(w,h,512,768);
   close(span(greeting,SeatedGreetingGeometry.FACE),unit,"Greeting shares standing eye unit");
   for(int pose=0;pose<ComfortPoseGeometry.ASSETS.length;pose++){
    float scale=ComfortPoseGeometry.scale(pose,w,h);
    close(scale*ComfortPoseGeometry.eyeSpan(pose),unit,"Comfort pose shares eye unit "+pose);
    float[] eyes=ComfortPoseGeometry.EYES[pose];
    close(ComfortPoseGeometry.originX(pose,w,scale)+(eyes[0]+eyes[2])*.5f*scale,w*.5f,"Comfort face centered "+pose);
    float crown=Math.max(0,ComfortPoseGeometry.FACE[pose][1]-ComfortPoseGeometry.FACE[pose][3]*.8f);
    close(ComfortPoseGeometry.originY(pose,h,scale)+crown*scale,CharacterViewport.headMargin(h),"Comfort crown retained "+pose);
   }
   for(int key=0;key<14;key++){
    int[] rect=key==9?new int[]{0,0,512,768}:RestArtworkLayout.bounds(key);
    float scale=RestArtworkLayout.scale(key,rect[3],w,h);
    close(scale*RestArtworkLayout.eyeSpan(key,rect[3]),unit,"Rest motion family shares eye unit "+key);
   }
  }
  // Wide pose: overflowing hands/legs must not cause a second fit-to-width.
  float[] narrow=CharacterViewport.fit(240,640,300,800,150,30,12);
  float[] wide=CharacterViewport.fit(240,640,1400,800,700,30,12);
  close(narrow[2]/300,wide[2]/1400,"Source lateral extent does not reduce scale");
  ok(wide[0]<0&&wide[0]+wide[2]>240,"Both lateral extremes may crop");
  close(wide[0]+700*wide[2]/1400,120,"Face survives wide crop");
  float[] tall=CharacterViewport.fit(240,320,400,2000,200,30,10);
  ok(tall[1]+tall[3]>320,"Feet may crop instead of shrinking anatomy");
  close(tall[1]+10*tall[2]/400,CharacterViewport.headMargin(320),"Head wins over feet");
  // Adding blank source padding must not move the visible face or change scale.
  float[] padded=CharacterViewport.fit(240,640,500,1000,250,30,112);
  close(narrow[0]+150*narrow[2]/300,padded[0]+250*padded[2]/500,"Horizontal padding invariant");
  close(narrow[1]+40*narrow[3]/800,padded[1]+140*padded[3]/1000,"Vertical padding invariant");
  System.out.println("PASS "+checks+" standing/seat/rest shared scale, face center, crown priority, crop and decoder assertions");
 }
}
