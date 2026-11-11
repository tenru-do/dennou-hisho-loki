package com.example.rokidkeyboardbridge;
/** Regression checks for apparent size, decoded resolution and foot overflow. */
public final class SeatedDisplayScaleChecks {
 private static int count;
 private static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
 private static void near(float a,float b,String message){check(Math.abs(a-b)<.02f,message+": "+a+" vs "+b);}
 public static void main(String[] args){System.out.println("Seated display checks PASS "+run());}
 static int run(){
  count=0;
  for(long age:new long[]{-1,0,45000,90000,120000,Long.MAX_VALUE})
   check(ApprovedQuietPosePolicy.seated(0,false,true,age)==0,"ordinary seated images do not jump before/around the leg timeline");
  for(int[] size:new int[][]{{480,640},{320,900},{900,320},{240,320}}){
   float target=-1;
   for(int id:new int[]{0,1,2,7}){
    float[] f=ApprovedQuietPosePolicy.FACE[id];int iw=(int)f[0],ih=(int)f[1];
    float crown=ApprovedQuietPosePolicy.SEATED_VERTICAL[id][0],foot=ApprovedQuietPosePolicy.SEATED_VERTICAL[id][1];
    float[] box=ApprovedQuietPosePolicy.fit(id,size[0],size[1],iw,ih);
    float scale=box[2]/iw;
    near(scale,box[3]/ih,"uniform proportions");
    float span=(float)Math.hypot(f[8]-f[2],f[9]-f[3])*scale;
    if(target<0)target=span;else near(span,target,"same seated apparent distance");
    near(span,CharacterViewport.eyeSpan(size[0],size[1]),"seated distance matches ordinary standing");
    near(box[0]+(f[2]+f[8])*.5f*scale,size[0]*.5f,"face centered");
    check(box[1]+crown*scale>=CharacterViewport.headMargin(size[1])-.02f,"crown retained");
    float[] decoded=ApprovedQuietPosePolicy.fit(id,size[0],size[1],iw/2,ih/2);
    near(decoded[0]+f[2]/f[0]*(iw/2)*decoded[2]/(iw/2),box[0]+f[2]*scale,"downsample does not move eyes");
    near(decoded[1]+f[3]/f[1]*(ih/2)*decoded[3]/(ih/2),box[1]+f[3]*scale,"downsample preserves vertical placement");
    // Adding transparent right/bottom padding must not shrink the same anatomy.
    float[] padded=f.clone();padded[0]+=400;padded[1]+=500;
    float[] extra=ApprovedQuietPosePolicy.seatedFit(size[0],size[1],iw+400,ih+500,padded,crown,foot);
    near(extra[2]/(iw+400),scale,"transparent canvas cannot shrink body");
    near(extra[0],box[0],"padding does not shift face");
    float[] lowFoot=ApprovedQuietPosePolicy.seatedFit(size[0],size[1],iw,ih,f,crown,foot+10000);
    near(lowFoot[2]/iw,scale,"low foot cannot shrink body");
    check(lowFoot[1]+crown*scale>=CharacterViewport.headMargin(size[1])-.02f,"overflow keeps head");
    check(lowFoot[1]+(foot+10000)*scale>size[1],"feet may crop at stable scale");
   }
   for(int id:new int[]{3,4,5,6}){
    float[] f=ApprovedQuietPosePolicy.FACE[id];
    float[] a=ApprovedQuietPosePolicy.fit(id,size[0],size[1],(int)f[0],(int)f[1]);
    float scale=a[2]/f[0];
    near(CharacterViewport.eyes(f)*scale,CharacterViewport.eyeSpan(size[0],size[1]),"resting distance matches standing");
    near(scale,a[3]/f[1],"resting uniform scale");
    near(a[0]+(f[2]+f[8])*.5f*scale,size[0]*.5f,"resting face stays centered");
    near(a[1],CharacterViewport.headMargin(size[1]),"resting source top retained");
   }
  }
  return count;
 }
}
