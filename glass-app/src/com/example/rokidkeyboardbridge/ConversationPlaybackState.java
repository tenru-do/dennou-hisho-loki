package com.example.rokidkeyboardbridge;
/** Chunk-boundary resume; never controls another generation's audio. */
public final class ConversationPlaybackState {
 private String id="";private String[] chunks=new String[0];private int generation=-1,index=0;private boolean paused=false;
 public synchronized void begin(String id,String[] chunks,int generation){this.id=id;this.chunks=chunks.clone();this.generation=generation;index=0;paused=false;}
 public synchronized void mark(int generation,int index){if(this.generation==generation)this.index=Math.max(0,Math.min(chunks.length-1,index));}
 public synchronized void check(String id,int generation){if(!this.id.equals(id)||this.generation!=generation||chunks.length==0)throw new IllegalArgumentException("この回答の読み上げ状態は変わりました。最新の回答で操作してください");}
 public synchronized void pause(String id,int before,int after){check(id,before);paused=true;generation=after;}
 public synchronized String text(String id,int generation,boolean replay){check(id,generation);if(!replay&&!paused)throw new IllegalArgumentException("一時停止した回答だけを再開できます");StringBuilder text=new StringBuilder();for(int i=replay?0:index;i<chunks.length;i++){if(text.length()>0)text.append('\n');text.append(chunks[i]);}return text.toString();}
}
