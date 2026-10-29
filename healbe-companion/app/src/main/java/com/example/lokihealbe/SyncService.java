package com.example.lokihealbe;
import android.app.*;import android.content.*;import android.os.*;import android.net.*;
public final class SyncService extends Service {
 private final Handler handler=new Handler(Looper.getMainLooper());private boolean running;
 private final Runnable tick=new Runnable(){public void run(){if(!getSharedPreferences("link",0).getBoolean("linked",false)){stopSelf();return;}handler.postDelayed(this,300000);ConnectivityManager cm=getSystemService(ConnectivityManager.class);NetworkCapabilities net=cm.getNetworkCapabilities(cm.getActiveNetwork());if(net==null||!net.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))return;new Thread(()->CloudSync.refresh(SyncService.this),"HEALBE-5min-sync").start();}};
 public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("healbe-sync","GoBe定期同期",NotificationManager.IMPORTANCE_LOW));Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE);Notification notice=new Notification.Builder(this,"healbe-sync").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("ロキ：GoBe収支の同期").setContentText("約5分ごとに取得。タップして連携を停止できます").setContentIntent(pi).setOngoing(true).build();startForeground(695,notice);}
 public int onStartCommand(Intent i,int flags,int id){if(!running){running=true;handler.post(tick);}return START_STICKY;}
 public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
 static void start(Context c){c.startForegroundService(new Intent(c,SyncService.class));}
}
