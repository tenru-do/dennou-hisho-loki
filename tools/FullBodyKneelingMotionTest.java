package com.example.rokidkeyboardbridge;
import android.graphics.*;import java.util.zip.ZipFile;
public final class FullBodyKneelingMotionTest {
 public static void main(String[] args)throws Exception{
  long bytes=0;float min=Float.MAX_VALUE,max=0;
  Bitmap proof=Bitmap.createBitmap(1200,616,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(proof);
  try(ZipFile zip=new ZipFile(args[0])){
   for(int id=288;id<=292;id++){
    Bitmap b=FullBodyRealisticAssets.decode(zip,id,2);if(b==null)throw new AssertionError("decode "+id);
    float scale=FullBodyScalePolicy.scale(id,b.getWidth(),b.getHeight(),240,616);
    float left=FullBodyAnchorPolicy.left(id,b.getWidth(),240,scale,1);
    if(left<-.01f||left+b.getWidth()*scale>240.01f||b.getHeight()*scale>616)throw new AssertionError("clipped full body "+id);
    float span=FullBodyScalePolicy.HEAD_SPAN[id]*b.getWidth()/FullBodyRealisticFeatures.ALL[id][0]*scale;
    min=Math.min(min,span);max=Math.max(max,span);if(span<FullBodyScalePolicy.commonSpan(240,616)*.85f)throw new AssertionError("distance changed "+id);
    canvas.drawBitmap(b,null,new RectF((id-288)*240+left,616-b.getHeight()*scale,(id-288)*240+left+b.getWidth()*scale,616),null);
    bytes+=b.getAllocationByteCount();b.recycle();
   }
  }
  if(max/min>1.12f)throw new AssertionError("scale drift");
  try(java.io.FileOutputStream f=new java.io.FileOutputStream("/data/local/tmp/loki-kneeling-v460.png")){proof.compress(Bitmap.CompressFormat.PNG,100,f);}
  System.out.println("PASS five full-body kneeling crops; uncut hair/knees/shoes; head span="+min+".."+max+"; unique bitmap bytes="+bytes);
 }
}
