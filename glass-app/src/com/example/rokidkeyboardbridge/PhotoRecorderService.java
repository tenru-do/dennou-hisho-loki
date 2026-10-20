package com.example.rokidkeyboardbridge;
import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
/** User-started camera foreground service; no microphone or Gemini calls. */
public final class PhotoRecorderService extends Service {
 static volatile PhotoRecorderService active; static volatile String status="";
 private volatile boolean stopped; private Thread worker; private PhotoArchive archive;
 private int interval=5000;private volatile long effectiveInterval=5000;private PowerManager.WakeLock wake;
 private final java.util.concurrent.locks.ReentrantLock imageWork=new java.util.concurrent.locks.ReentrantLock(true);
 private volatile String topicImageDecision="";
 String topicImageWaitReason(){return MeetingCameraPolicy.label(topicImageDecision);}
 private volatile boolean captureWaiting;private File latestPhoto;private long latestPhotoAt,latestPhotoWall,lastTopicUse;
 static final class TopicPhoto {final byte[] jpeg;final long capturedAt,ageMs;TopicPhoto(byte[] jpeg,long at,long age){this.jpeg=jpeg;capturedAt=at;ageMs=age;}}
 TopicPhoto topicPhoto(java.util.function.BooleanSupplier cancelled)throws Exception {
  String load=ResourceLoad.optional(this);if(!load.isEmpty()){topicImageDecision="RESOURCE_LOAD";return null;}
  if(stopped||active!=this){topicImageDecision="CANCELLED";return null;}if(captureWaiting||!imageWork.tryLock()){topicImageDecision="KEEP_WAIT";return null;}
  try {
   if(stopped||active!=this||cancelled.getAsBoolean()){topicImageDecision="CANCELLED";return null;}if(latestPhoto==null){topicImageDecision="NO_FRAME";return null;}
   MeetingRecorderService meeting=MeetingRecorderService.active;if(meeting!=null){String blocked=meeting.optionalImageBlockReason();if(!blocked.isEmpty()){topicImageDecision=blocked;return null;}}
   android.app.ActivityManager.MemoryInfo info=new android.app.ActivityManager.MemoryInfo();
   ((android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);
   Runtime runtime=Runtime.getRuntime();long free=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory()),now=SystemClock.elapsedRealtime();
   topicImageDecision=KeepTopicImagePolicy.reason(now,latestPhotoAt,lastTopicUse,latestPhoto.length(),free,info.lowMemory,captureWaiting);if(!topicImageDecision.isEmpty())return null;
   byte[] source=archive.read(latestPhoto);if(cancelled.getAsBoolean()){topicImageDecision="CANCELLED";return null;}if(captureWaiting){topicImageDecision="KEEP_WAIT";return null;}
   byte[] small=ImageQuestionBitmap.normalize(source,640,250000);
   if(stopped||active!=this||cancelled.getAsBoolean()){topicImageDecision="CANCELLED";return null;}
   long finished=SystemClock.elapsedRealtime();if(finished<latestPhotoAt||finished-latestPhotoAt>15000){topicImageDecision="STALE";return null;}
   topicImageDecision="READY";lastTopicUse=finished;return new TopicPhoto(small,latestPhotoWall,finished-latestPhotoAt);
  }catch(OutOfMemoryError pressure){topicImageDecision="LOW_MEMORY";return null;
  }catch(Exception error){topicImageDecision="READ_ERROR";throw error;
  }finally{imageWork.unlock();}
 }
 TopicPhoto awaitTopicPhoto(java.util.function.BooleanSupplier cancelled)throws Exception {
  long until=SystemClock.elapsedRealtime()+2000;
  do{if(cancelled.getAsBoolean())throw new InterruptedException("observation cancelled");TopicPhoto value=topicPhoto(cancelled);if(value!=null)return value;
   if(!topicImageDecision.matches("KEEP_WAIT|NO_FRAME|STALE"))return null;
   if(SystemClock.elapsedRealtime()>=until)return null;Thread.sleep(100);
  }while(true);
 }
 public IBinder onBind(Intent intent){return null;}
 public void onCreate(){super.onCreate();
  try{archive=PhotoArchiveStore.get(this);}
  catch(Exception e){status=e.getMessage();stopSelf();return;}
  NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel("loki_recorder","連続撮影",NotificationManager.IMPORTANCE_LOW));
  wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"loki:recorder");
 }
 private Notification notification(){
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PhotoRecorderService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,"loki_recorder").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("ロキ：連続撮影中").setContentText(effectiveInterval/1000+"秒間隔・上限5GB / "+status).setOngoing(true).addAction(0,"停止",stop).build();
 }
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null||archive==null){stopSelf();return START_NOT_STICKY;}
  if("stop".equals(intent.getAction())){requestStop();return START_NOT_STICKY;}
  if("keep".equals(intent.getAction())){return START_NOT_STICKY;}
  interval=intent.getIntExtra("interval",5000)==10000?10000:5000;
  startForeground(442,notification());active=this;
  if(worker==null){wake.acquire();worker=new Thread(this::loop,"LokiPhotoRecorder");worker.start();}
  return START_NOT_STICKY;
 }
 void requestStop(){
  // Cancel first; never wait for archive I/O or camera teardown on the UI thread.
  stopped=true;status="ユーザー操作で停止";
  Thread running=worker;if(running!=null)running.interrupt();
  stopSelf();
 }
 private void loop(){try{archive.encryptLegacy();}catch(Exception e){status="既存写真の暗号化を完了できません。撮影停止";stopSelf();return;}int failures=0;long next=SystemClock.elapsedRealtime();
  while(!stopped){try{
   long wait=next-SystemClock.elapsedRealtime();if(wait>0)Thread.sleep(wait);if(stopped)break;int load=ResourceLoad.level(this);effectiveInterval=ResourceLoadPolicy.interval(interval,load);next=SystemClock.elapsedRealtime()+effectiveInterval;if(load>=2){status=ResourceLoad.status;((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(442,notification());continue;}
   // Other local camera features take priority; missed slots are never queued.
   if(CaptureLease.CAMERA.availablePermits()==0){status="カメラ使用中・撮影待ち";continue;}
   MeetingRecorderService meeting=MeetingRecorderService.active;if(meeting!=null&&!meeting.reserveCamera()){status="会議録音優先："+meeting.cameraWaitReason();continue;}
   captureWaiting=true;try{imageWork.lockInterruptibly();}finally{captureWaiting=false;}
   try {
   if(stopped)break;
   long captureStarted=SystemClock.elapsedRealtime(),captureWall=System.currentTimeMillis();byte[] jpeg;MainActivity host=MainActivity.recorderHost.get();
   RecorderPhotoLocation.refresh(host);
   try(AutoCloseable pause=host!=null&&!host.isDestroyed()?host.pauseMascotForCamera():()->{}){
    jpeg=TopicObservationCapture.recorderPhoto(this,()->stopped);
   }
   if(stopped)break;long captureMs=SystemClock.elapsedRealtime()-captureStarted;long saveStarted=SystemClock.elapsedRealtime();long capturedAt=System.currentTimeMillis();java.io.File saved=archive.save(jpeg,capturedAt);archive.metadata(saved,RecorderPhotoLocation.snapshot(capturedAt));failures=0;android.util.Log.i("LokiRecorderTiming","capture_ms="+captureMs+" save_ms="+(SystemClock.elapsedRealtime()-saveStarted)+" interval_ms="+interval);
   if(!stopped){MainActivity feedbackHost=MainActivity.recorderHost.get();if(feedbackHost!=null)feedbackHost.onKeepPhotoSaved();}
   latestPhoto=saved;latestPhotoAt=captureStarted;latestPhotoWall=captureWall;
   long[] stats=archive.stats();status="REC "+stats[0]+"枚 / "+String.format(java.util.Locale.US,"%.2fGB",stats[1]/1e9)+" / 保護"+stats[2]+"枚"+(ResourceLoad.status.equals("通常負荷")?"":" / "+ResourceLoad.status);
   }finally{imageWork.unlock();}
  }catch(InterruptedException e){if(stopped)break;status="カメラ使用中・撮影待ち";}
   catch(IOException e){status=e.getMessage();stopSelf();break;}
   catch(Exception e){status="撮影失敗（"+(++failures)+"）："+e.getClass().getSimpleName();if(failures>=5){stopSelf();break;}}
   if(!stopped)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(442,notification());
  }
 }
 public void onDestroy(){stopped=true;if(active==this)active=null;if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();MainActivity host=MainActivity.recorderHost.get();if(host!=null)host.onPhotoRecorderStopped(status);stopForeground(true);super.onDestroy();}
}
