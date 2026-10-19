package com.example.lokimotionreview;
import android.app.*;import android.os.*;import dalvik.system.PathClassLoader;import java.lang.reflect.*;import java.io.*;
public final class Audit extends Instrumentation {
 String selected="all",run="";
 public void onCreate(Bundle b){super.onCreate(b);if(b!=null){selected=b.getString("poses","all");run=b.getString("run","");}start();}
 public void onStart(){Bundle out=new Bundle();try{
  if(!run.matches("[a-f0-9]{12}"))throw new IllegalArgumentException("Invalid export id");
  File dir=new File(getTargetContext().getCacheDir(),"motion-review-"+run);
  PathClassLoader loader=new PathClassLoader("/data/local/tmp/loki-motion-review.dex:"+getTargetContext().getApplicationInfo().sourceDir,ClassLoader.getSystemClassLoader());
  // No activity attachment, capture, microphone, network requests or shared gallery writes.
  Class<?> export=loader.loadClass("com.example.rokidkeyboardbridge.RenderExport");
  runOnMainSync(()->{});
  export.getMethod("run",android.content.Context.class,String.class,String.class).invoke(null,getTargetContext(),dir.getPath(),selected);
  out.putString("review","PASS native frames generated");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("failure",(e instanceof InvocationTargetException?e.getCause():e).toString());finish(Activity.RESULT_CANCELED,out);}}
}
