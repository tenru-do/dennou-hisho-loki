package com.example.rokidgeminisecretary;
import android.app.*;import android.content.*;import android.os.*;
public final class MeetingProcessingService extends Service {
 private volatile Thread worker;private PowerManager.WakeLock wake;
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("meeting_processing","会議記録の後処理",NotificationManager.IMPORTANCE_LOW));}
 public int onStartCommand(Intent i,int flags,int start){if(i==null){stopSelf();return START_NOT_STICKY;}if(worker!=null)return START_NOT_STICKY;String id=i.getStringExtra("id");boolean analysis=i.getBooleanExtra("analysis",false);startForeground(887,new Notification.Builder(this,"meeting_processing").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle(analysis?"ロキ：会議の文字起こし・AI要約中":"ロキ：録画・録音から音声抽出中").setContentText("PC・タブレットの会議記録で進行状況を確認できます").setOngoing(true).build());wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"loki:meeting-import");wake.acquire(5*60*60*1000L);worker=new Thread(()->{try{if(analysis)MeetingAudioStore.runAnalysis(this,id);else MeetingMediaImport.convert(this,id);}catch(Exception e){if(!analysis)MeetingMediaImport.failed(this,id,e);}finally{if(!analysis)MeetingMediaImport.active="";worker=null;new Handler(Looper.getMainLooper()).post(()->stopSelf(start));}},"MeetingMediaDecode");worker.start();return START_NOT_STICKY;}
 public void onDestroy(){if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();stopForeground(true);super.onDestroy();}
 public void onTimeout(int startId,int fgsType){if(worker!=null)worker.interrupt();stopSelf();}
}