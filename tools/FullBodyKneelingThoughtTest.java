package com.example.rokidkeyboardbridge;
import android.graphics.*;import java.util.zip.ZipFile;
public final class FullBodyKneelingThoughtTest {
 public static void main(String[] args)throws Exception{
  long bytes=0;Bitmap proof=Bitmap.createBitmap(1440,1232,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(proof);
  try(ZipFile zip=new ZipFile(args[0])){
   for(int first:new int[]{293,299}){
    float lo=Float.MAX_VALUE,hi=0;
    for(int id=first;id<first+6;id++){
     Bitmap b=FullBodyRealisticAssets.decode(zip,id,2);if(b==null)throw new AssertionError("decode "+id);
     float s=FullBodyScalePolicy.scale(id,b.getWidth(),b.getHeight(),240,616),left=FullBodyAnchorPolicy.left(id,b.getWidth(),240,s,1),h=b.getHeight()*s;
     if(left<-.01f||left+b.getWidth()*s>240.01f||h>616)throw new AssertionError("clipped "+id);
     lo=Math.min(lo,h);hi=Math.max(hi,h);int x=(id-first)*240,y=first==293?0:616;
     c.drawBitmap(b,null,new RectF(x+left,y+616-h,x+left+b.getWidth()*s,y+616),null);bytes+=b.getAllocationByteCount();b.recycle();
    }
    if(hi/lo>1.04f)throw new AssertionError("pose size drift "+first);
   }
  }
  try(java.io.FileOutputStream f=new java.io.FileOutputStream("/data/local/tmp/loki-kneeling-thought-v461.png")){proof.compress(Bitmap.CompressFormat.PNG,100,f);}
  System.out.println("PASS 12 full-body thought frames; head/knees/shoes uncut; body height drift <4 percent; bytes="+bytes);
 }
}
