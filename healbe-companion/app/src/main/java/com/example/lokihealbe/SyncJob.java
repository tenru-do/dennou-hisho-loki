package com.example.lokihealbe;
import android.app.job.*;import android.content.*;
public final class SyncJob extends JobService {
 private Thread worker;
 public boolean onStartJob(JobParameters params){worker=new Thread(()->{CloudSync.refresh(this);jobFinished(params,false);},"HEALBE-SDK-sync");worker.start();return true;}
 public boolean onStopJob(JobParameters params){if(worker!=null)worker.interrupt();return false;}
 static void schedule(Context c){try{SyncService.start(c);c.getSystemService(JobScheduler.class).cancel(694);}catch(Exception unavailable){c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(694,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true).setPeriodic(900000,300000).setPersisted(true).build());}}
}
