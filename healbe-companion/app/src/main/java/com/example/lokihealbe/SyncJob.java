package com.example.lokihealbe;
import android.app.job.*;import android.content.*;
public final class SyncJob extends JobService {
 private Thread worker;
 public boolean onStartJob(JobParameters params){long now=System.currentTimeMillis(),last=getSharedPreferences("link",0).getLong("lastSuccessAt",0);if(!getSharedPreferences("link",0).getBoolean("linked",false)||last>0&&now>=last&&now-last<480000)return false;worker=new Thread(()->{CloudSync.refresh(this);jobFinished(params,false);},"HEALBE-SDK-sync");worker.start();return true;}
 public boolean onStopJob(JobParameters params){if(worker!=null)worker.interrupt();return false;}
 static void registerFallback(Context c){if(!c.getSharedPreferences("link",0).getBoolean("linked",false))return;c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(694,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true).setPeriodic(900000,300000).setPersisted(true).build());}
 static void schedule(Context c){registerFallback(c);try{SyncService.start(c);}catch(Exception unavailable){/* Persistent job remains registered when foreground start is restricted. */}}
}
