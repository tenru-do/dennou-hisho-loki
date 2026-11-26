package com.example.rokidgeminisecretary;
import android.app.*;import android.content.*;import android.os.*;
public final class MeetingProcessingService extends Service {
 private volatile Thread worker;private PowerManager.WakeLock wake;
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("meeting_processing","会議記録の後処理",NotificationManager.IMPORTANCE_LOW));}
 public int onStartCommand(Intent i,int flags,int start){stopSelf();return START_NOT_STICKY;}
 public void onDestroy(){if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();stopForeground(true);super.onDestroy();}
 public void onTimeout(int startId,int fgsType){if(worker!=null)worker.interrupt();stopSelf();}
}