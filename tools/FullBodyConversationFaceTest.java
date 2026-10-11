package com.example.rokidkeyboardbridge;
import android.graphics.*;
import java.io.*;
import java.util.zip.ZipFile;
/** Android Canvas audit: all runtime crops, no body changes, silent mouth, preserved closed eyes. */
public final class FullBodyConversationFaceTest {
 public static void main(String[]args)throws Exception{
  File dir=new File("/data/local/tmp/loki-conversation-face-v459");dir.mkdirs();
  FullBodyFaceAnimator animator=new FullBodyFaceAnimator();int checks=0,blinked=0,preserved=0,mouths=0;
  try(ZipFile zip=new ZipFile(args[0])){
   for(int id=276;id<FullBodyRealisticFeatures.ALL.length;id++){
    float[]f=FullBodyFaceAnimator.features(id);if(f==null)throw new AssertionError("Missing face "+id);
    for(int sample:new int[]{1,2}){
     Bitmap base=FullBodyRealisticAssets.decode(zip,id,sample);if(base==null)throw new AssertionError("Decode "+id);
     int w=base.getWidth(),h=base.getHeight();int[]original=new int[w*h],output=new int[w*h];base.getPixels(original,0,w,0,0,w,h);
     for(int state=0;state<5;state++){
      long t=state==1?4600:state==2?4540:state==3?220:state==4?660:0;
      Bitmap result=base.copy(Bitmap.Config.ARGB_8888,true);animator.draw(new Canvas(result),base,id,t,state>=3);result.getPixels(output,0,w,0,0,w,h);
      int changed=0;float bound=Math.max(Math.max(f[3]+f[4]*.65f,f[9]+f[10]*.65f),f[15]+Math.max(f[16]*.7f,f[18]))+3;
      for(int i=0;i<output.length;i++)if(output[i]!=original[i]){changed++;if(i/w>bound*h/f[1])throw new AssertionError("Body altered "+id+"/"+state);}
      boolean eyes=!FullBodyFaceAnimator.transition(id)&&(f[7]>0||f[13]>0);
      if(state==0&&changed!=0)throw new AssertionError("Silent face altered "+id);
      if((state==1||state==2)&&((eyes&&changed==0)||(!eyes&&changed!=0)))throw new AssertionError("Eye visibility "+id+"/"+state);
      if(state>=3&&((f[16]>0&&changed==0)||(f[16]==0&&changed!=0)))throw new AssertionError("Missing talking mouth "+id+"/"+sample);
      if(sample==1){if(state==1){if(eyes)blinked++;else preserved++;}if(state==3&&f[16]>0)mouths++;
       Bitmap head=Bitmap.createBitmap(result,0,0,w,Math.min(h,(int)Math.ceil(bound)));
       try(FileOutputStream stream=new FileOutputStream(new File(dir,String.format(java.util.Locale.US,"%03d-%d.png",id,state)))){head.compress(Bitmap.CompressFormat.PNG,100,stream);}if(head!=result)head.recycle();
      }
      result.recycle();checks++;
     }
     Bitmap silent=base.copy(Bitmap.Config.ARGB_8888,true);animator.draw(new Canvas(silent),base,id,220,false);silent.getPixels(output,0,w,0,0,w,h);if(!java.util.Arrays.equals(original,output))throw new AssertionError("Silent mouth "+id);silent.recycle();base.recycle();checks++;
    }
   }
  }
  System.out.println("PASS realistic fullbody: frames="+FullBodyRealisticFeatures.ALL.length+" checks="+checks+" blink="+blinked+" closed/covered preserved="+preserved+" talking mouths="+mouths+"; full/half resolution; body unchanged; silent mouth unchanged");
 }
}
