package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;import java.util.concurrent.*;
/** Capture and storage are independent of TOPIC, camera and network workers. */
public final class MeetingRecorderService extends Service {
    static volatile MeetingRecorderService active;static volatile String status="会議録音停止中";
    int mode;
    boolean isStopping(){return stopping;}
    private volatile boolean stopping,done;private volatile String reason="終了操作";
    private final ArrayBlockingQueue<byte[]> queue=new ArrayBlockingQueue<>(64);
    private final ArrayBlockingQueue<org.json.JSONObject> topics=new ArrayBlockingQueue<>(100);
    private final MeetingPcmRing ring=new MeetingPcmRing();private PowerManager.WakeLock wake;
    private long started,lastCamera;private volatile long captured,saved,speechUntil;private volatile int level;
    public IBinder onBind(Intent i){return null;}
    public void onCreate(){super.onCreate();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("loki_meeting","ロキの会議録音",NotificationManager.IMPORTANCE_LOW));}
    private Notification notification(){PendingIntent stop=PendingIntent.getActivity(this,882,new Intent(this,MainActivity.class).setAction("loki.meeting.controls"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"loki_meeting").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("ロキ：会議録音中（最大2時間）").setContentText(status).setOngoing(true).setContentIntent(stop).build();}
    public int onStartCommand(Intent i,int flags,int id){if(i==null){stopSelf();return START_NOT_STICKY;}if(active!=null)return START_NOT_STICKY;
        mode=Math.max(0,Math.min(3,i.getIntExtra("meetingMode",0)));
        started=SystemClock.elapsedRealtime();active=this;status="会議録音：開始準備中";
        if(Build.VERSION.SDK_INT>=29){int types=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;if(checkSelfPermission("android.permission.CAMERA")==0)types|=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;startForeground(883,notification(),types);}else startForeground(883,notification());
        wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"loki:meeting");wake.acquire(2*60*60*1000L+60000);
        new Thread(this::store,"MeetingDisk").start();return START_NOT_STICKY;}
    void requestStop(){reason="終了操作";stopping=true;}
    void assistantSpeech(long ms){speechUntil=SystemClock.elapsedRealtime()+ms;}
    boolean recentAssistantSpeech(){return speechUntil>0&&SystemClock.elapsedRealtime()-speechUntil<4000;}
    void noteTopic(String text){try{topics.offer(new org.json.JSONObject().put("at",System.currentTimeMillis()).put("audioMs",captured/32).put("text",text.length()>5000?text.substring(0,5000):text));}catch(Exception ignored){}}
    byte[] observation()throws Exception {byte[] b=ring.snapshot();if(b.length<3200)throw new IllegalStateException("会議音声の準備中");return b;}
    synchronized boolean reserveCamera(){if(mode==0||mode==3)return false;long now=SystemClock.elapsedRealtime();ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(info);if(stopping||queue.size()>16||info.lowMemory||now-lastCamera<30000)return false;lastCamera=now;return true;}
    private void capture(){AudioRecord mic=null;
        try(CaptureLease lease=CaptureLease.acquire(CaptureLease.MICROPHONE,()->stopping)){
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("マイク初期化失敗");mic=new AudioRecord(MediaRecorder.AudioSource.DEFAULT,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,64000));MicrophoneRouting.preferUsb(this,mic);mic.startRecording();
            if(mic.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("マイク開始失敗");MainActivity host=MainActivity.recorderHost.get();if(host!=null)host.onMeetingRecorderStarted(this);byte[] b=new byte[4096];long notifyAt=0,lastData=SystemClock.elapsedRealtime();
            while(!stopping&&captured<MeetingAudioArchive.MAX_BYTES&&SystemClock.elapsedRealtime()-started<7200000L){int n=mic.read(b,0,(int)Math.min(b.length,MeetingAudioArchive.MAX_BYTES-captured),AudioRecord.READ_NON_BLOCKING);
                if(n<0)throw new IllegalStateException("マイク読取エラー "+n);long now=SystemClock.elapsedRealtime();
                if(n==0){if(now-lastData>5000)throw new IllegalStateException("マイク入力が5秒停止");Thread.sleep(5);continue;}lastData=now;
                byte[] frame=java.util.Arrays.copyOf(b,n);if(!queue.offer(frame))throw new IllegalStateException("保存処理の遅延：音声欠落を防ぐため停止");
                captured+=n;ring.put(frame,n);level=RadioPlaybackPolicy.level(frame,n);
                if(now-notifyAt>1000){status=String.format(java.util.Locale.JAPAN,"MTG "+new String[]{"録音のみ","音＋カメラ＋録音","音＋カメラ＋写真＋録音","音＋録音"}[mode]+" %02d:%02d / 120:00・保存%dKB・入力%d",captured/32000/60,captured/32000%60,saved/1024,level);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(883,notification());notifyAt=now;}
            }
            if(!stopping)reason="最大2時間に到達";
        }catch(Exception e){if(!stopping)reason=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();stopping=true;}
        finally{if(mic!=null){try{mic.stop();}catch(Exception ignored){}mic.release();}done=true;}
    }
    private void store(){try(MeetingAudioArchive archive=new MeetingAudioArchive(this)){
        new Thread(this::capture,"MeetingMic").start();
        while(!done||!queue.isEmpty()){byte[] b=queue.poll(200,TimeUnit.MILLISECONDS);if(b!=null){if(archive.folder.getUsableSpace()<50000000)throw new java.io.IOException("空き容量不足");archive.append(b);saved+=b.length;}org.json.JSONObject item;while((item=topics.poll())!=null)archive.topic(item);}
        archive.finish(reason);status="会議録音保存済み："+reason+" / "+saved/32000+"秒";
    }catch(Exception e){reason="保存エラー："+(e instanceof java.io.IOException?e.getMessage():e.getClass().getSimpleName());stopping=true;status=reason+"（保存済み区間は保持）";}
    finally{stopping=true;if(active==this)active=null;new Handler(Looper.getMainLooper()).post(this::stopSelf);}}
    public void onDestroy(){stopping=true;MainActivity host=MainActivity.recorderHost.get();if(host!=null)host.onMeetingRecorderEnded(mode);if(wake!=null&&wake.isHeld())wake.release();if(active==this)active=null;stopForeground(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(884,new Notification.Builder(this,"loki_meeting").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("ロキ：会議録音終了").setContentText(status).setAutoCancel(true).build());super.onDestroy();}
}
