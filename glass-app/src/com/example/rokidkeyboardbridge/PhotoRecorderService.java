package com.example.rokidkeyboardbridge;
import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
/** User-started camera foreground service; no microphone or Gemini calls. */
public final class PhotoRecorderService extends Service {
 static volatile PhotoRecorderService active; static volatile String status="";
 private volatile boolean stopped; private Thread worker; private PhotoArchive archive;
 private int interval=5000;private PowerManager.WakeLock wake;
 public IBinder onBind(Intent intent){return null;}
 public void onCreate(){super.onCreate();
  try{archive=PhotoArchiveStore.get(this);}
  catch(Exception e){status=e.getMessage();stopSelf();return;}
  NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel("loki_recorder","連続撮影",NotificationManager.IMPORTANCE_LOW));
  wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"loki:recorder");
 }
 private Notification notification(){
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PhotoRecorderService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent keep=PendingIntent.getService(this,2,new Intent(this,PhotoRecorderService.class).setAction("keep"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,"loki_recorder").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("ロキ：連続撮影中").setContentText(interval/1000+"秒間隔・上限5GB / "+status).setOngoing(true).addAction(0,"KEEP",keep).addAction(0,"停止",stop).build();
 }
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null||archive==null){stopSelf();return START_NOT_STICKY;}
  if("stop".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if("keep".equals(intent.getAction())){protect();return START_NOT_STICKY;}
  interval=intent.getIntExtra("interval",5000)==10000?10000:5000;
  startForeground(442,notification());active=this;
  if(worker==null){wake.acquire();worker=new Thread(this::loop,"LokiPhotoRecorder");worker.start();}
  return START_NOT_STICKY;
 }
 synchronized String protect(){try{int count=archive.protect(System.currentTimeMillis());status="保護：直前"+count+"枚＋次の1分";return status;}catch(Exception e){status="保護失敗："+e.getMessage();return status;}}
 private void loop(){try{archive.encryptLegacy();}catch(Exception e){status="既存写真の暗号化を完了できません。撮影停止";stopSelf();return;}int failures=0;long next=SystemClock.elapsedRealtime();
  while(!stopped){try{
   long wait=next-SystemClock.elapsedRealtime();if(wait>0)Thread.sleep(wait);if(stopped)break;next=SystemClock.elapsedRealtime()+interval;
   // Other local camera features take priority; missed slots are never queued.
   if(CaptureLease.CAMERA.availablePermits()==0){status="カメラ使用中・撮影待ち";continue;}
   byte[] jpeg;MainActivity host=MainActivity.recorderHost.get();
   try(AutoCloseable pause=host!=null&&!host.isDestroyed()?host.pauseMascotForCamera():()->{}){
    jpeg=TopicObservationCapture.recorderPhoto(this,()->stopped);
   }
   if(stopped)break;archive.save(jpeg,System.currentTimeMillis());failures=0;
   long[] stats=archive.stats();status="REC "+stats[0]+"枚 / "+String.format(java.util.Locale.US,"%.2fGB",stats[1]/1e9)+" / 保護"+stats[2]+"枚";
  }catch(InterruptedException e){if(stopped)break;status="カメラ使用中・撮影待ち";}
   catch(IOException e){status=e.getMessage();stopSelf();break;}
   catch(Exception e){status="撮影失敗（"+(++failures)+"）："+e.getClass().getSimpleName();if(failures>=5){stopSelf();break;}}
   if(!stopped)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(442,notification());
  }
 }
 public void onDestroy(){stopped=true;if(active==this)active=null;if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();MainActivity host=MainActivity.recorderHost.get();if(host!=null)host.onPhotoRecorderStopped(status);stopForeground(true);super.onDestroy();}
}
