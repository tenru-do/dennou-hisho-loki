package com.example.rokidkeyboardbridge;
import org.json.*;
/** Location refresh never blocks capture; stale samples are omitted. */
final class RecorderPhotoLocation {
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();private static final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean();private static volatile String cache="{}";private static volatile long received;private static long requested;
 static void refresh(MainActivity host){long now=android.os.SystemClock.elapsedRealtime();if(host==null||host.isDestroyed()||now-requested<15000||!busy.compareAndSet(false,true))return;requested=now;worker.execute(()->{try{String value=host.recorderLocationJson();new JSONObject(value);cache=value;received=android.os.SystemClock.elapsedRealtime();}catch(Exception e){cache="{}";}finally{busy.set(false);}});}
 static byte[] snapshot(long at)throws Exception{JSONObject meta=new JSONObject().put("capturedAt",at).put("available",false);JSONObject location=new JSONObject(cache);long age=at-location.optLong("locationAt",0);
  if(android.os.SystemClock.elapsedRealtime()-received<30000&&location.optBoolean("available")&&age>=0&&age<=90000){meta=location;meta.put("capturedAt",at);}return meta.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
 }
 static byte[] readSnapshot(MainActivity host,long at,java.util.function.BooleanSupplier cancelled)throws Exception {
  refresh(host);byte[] cached=snapshot(at);if(new JSONObject(new String(cached,java.nio.charset.StandardCharsets.UTF_8)).optBoolean("available"))return cached;
  long until=android.os.SystemClock.elapsedRealtime()+1200;
  while(busy.get()&&android.os.SystemClock.elapsedRealtime()<until){if(cancelled.getAsBoolean())throw new InterruptedException("READ cancelled");Thread.sleep(25);}
  if(cancelled.getAsBoolean())throw new InterruptedException("READ cancelled");return snapshot(at);
 }
}
