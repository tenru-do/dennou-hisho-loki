package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.util.Log;
import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.*;
/** Bounded diagnostics with hashes linking to the existing conversation history. */
final class FullBodyReactionAudit {
 private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"LokiReactionAudit");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardPolicy());
 static{worker.allowCoreThreadTimeOut(true);}
 private static String last="";
 static void recordPortrait(Context context,String prompt,String answer,FullBodySpeechStance.Cue cue,int expression){
  FullBodySemanticReaction.Reaction model=FullBodySemanticReaction.find(answer);
  boolean corrected=model!=null&&cue!=null&&model.cue.kind!=cue.kind;
  String expected=cue==null?"UNKNOWN":cue.kind.name();
  String line="{\"time\":"+System.currentTimeMillis()+",\"turnHash\":\""+hash(prompt+"\n"+answer)+"\",\"expected\":\""+expected+"\",\"expression\":"+expression+",\"mismatch\":"+(expression<0)+",\"corrected\":"+corrected+"}\n";
  Log.i("LokiPortraitReaction","expected="+expected+" expression="+expression+" corrected="+corrected);
  File file=new File(context.getApplicationContext().getFilesDir(),"portrait-reaction-audit.jsonl");
  worker.execute(()->{try{if(file.length()>262144){File old=new File(file.getParentFile(),file.getName()+".previous");if(old.exists()&&!old.delete())return;if(!file.renameTo(old))return;}try(FileOutputStream out=new FileOutputStream(file,true)){out.write(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(IOException e){Log.w("LokiPortraitReaction","audit unavailable",e);}});
 }
 static synchronized void record(Context context,String prompt,String answer,int pose,FullBodySpeechStance.Cue model,FullBodySpeechStance.Cue local){
  String expected=local!=null?local.kind.name():model!=null?model.kind.name():"UNKNOWN";
  boolean mismatch=pose<0||local==null&&model==null||pose>=FullBodyPoseCatalog.ALL.length||!"UNKNOWN".equals(expected)&&!FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.valueOf(expected));
  boolean corrected=model!=null&&local!=null&&model.kind!=local.kind&&pose>=0&&pose<FullBodyPoseCatalog.ALL.length&&FullBodyPoseCatalog.ALL[pose].supports(local.kind)&&!FullBodyPoseCatalog.ALL[pose].supports(model.kind);
  Log.i("LokiReactionAudit","expected="+expected+" pose="+pose+" mismatch="+mismatch+" corrected="+corrected);
  String key=hash(prompt+"\n"+answer)+":"+pose+":"+expected;if(key.equals(last))return;last=key;
  File file=new File(context.getApplicationContext().getFilesDir(),"fullbody-reaction-audit.jsonl");
  String line="{\"time\":"+System.currentTimeMillis()+",\"turnHash\":\""+hash(prompt+"\n"+answer)+"\",\"expected\":\""+expected+"\",\"pose\":"+pose+",\"mismatch\":"+mismatch+",\"corrected\":"+corrected+",\"model\":\""+(model==null?"NONE":model.kind)+"\"}\n";
  worker.execute(()->{try{if(file.length()>262144){File old=new File(file.getParentFile(),file.getName()+".previous");if(old.exists()&&!old.delete())return;if(!file.renameTo(old))return;}try(FileOutputStream out=new FileOutputStream(file,true)){out.write(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(IOException e){Log.w("LokiReactionAudit","audit unavailable",e);}});
 }
 private static String hash(String text){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte n:b)s.append(String.format(java.util.Locale.US,"%02x",n&255));return s.toString();}catch(Exception e){return "unavailable";}}
}
