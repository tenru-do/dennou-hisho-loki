package com.example.rokidkeyboardbridge;
import java.util.zip.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;
public final class FullBodyRestingAssetsTest{
 public static void main(String[]args)throws Exception{
  int count=FullBodyRealisticFeatures.ALL.length;
  if(count!=335||FullBodyRealisticAssets.REGIONS.length!=count||FullBodyRealisticAssets.SHEET.length!=count||FullBodyAnchorPolicy.X.length!=count||FullBodyAnchorPolicy.MIN.length!=count||FullBodyAnchorPolicy.MAX.length!=count||FullBodyScalePolicy.HEAD_SPAN.length!=count)throw new AssertionError("Landmark table coverage");
  int checked=0;long maxBytes=0;
  try(ZipFile apk=new ZipFile(args[0])){
   for(int link=4;link<6;link++){
    int from=FullBodyPoseLinks.PAIRS[link][0],to=FullBodyPoseLinks.PAIRS[link][1];
    if(FullBodyPoseLinks.index(from,to)!=link||FullBodyPoseLinks.index(to,from)!=link||FullBodyPoseLinks.reverse(link,from)||!FullBodyPoseLinks.reverse(link,to))throw new AssertionError("Forward/reverse route");
    long bytes=0;
    for(int id:FullBodyPoseLinks.frames(link)){
     String name=FullBodyRealisticAssets.NAMES[FullBodyRealisticAssets.SHEET[id]];
     BufferedImage image;try(java.io.InputStream in=apk.getInputStream(apk.getEntry("assets/"+name))){image=ImageIO.read(in);}
     int[] r=FullBodyRealisticAssets.REGIONS[id];float[] f=FullBodyRealisticFeatures.ALL[id];
     if(r[0]<0||r[1]<0||r[2]>image.getWidth()||r[3]>image.getHeight()||r[2]-r[0]!=(int)f[0]||r[3]-r[1]!=(int)f[1])throw new AssertionError("Crop mismatch "+id);
     for(int offset:new int[]{2,8,14})if(f[offset]<0||f[offset]>=f[0]||f[offset+1]<0||f[offset+1]>=f[1])throw new AssertionError("Face outside crop "+id);
     for(int height:new int[]{299,598}){float scale=FullBodyScalePolicy.scale(id,(int)f[0],(int)f[1],240,height);if(!(scale>0)||scale*(FullBodyAnchorPolicy.MAX[id]-FullBodyAnchorPolicy.MIN[id])>240.01f||scale*f[1]>height+.01f)throw new AssertionError("View overflow "+id);}
     bytes+=(long)f[0]*(long)f[1]*4;checked++;
    }
    if(bytes>8000000)throw new AssertionError("Transition cache excessive");maxBytes=Math.max(maxBytes,bytes);
   }
  }
  if(FullBodyPoseLinks.index(168,1)>=0||FullBodyPoseLinks.index(-1,1)>=0)throw new AssertionError("Unexpected startup/seated route");
  System.out.println("PASS "+checked+" packaged intermediate frames, crop/face/scale/anchor coverage, four new directions; max one-link bytes="+maxBytes);
 }
}
