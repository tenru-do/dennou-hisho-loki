package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.PowerManager;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/** Segments a real microphone without waiting for the previous segment's recognition. */
final class VoicePcmCapture implements ContinuousVoiceInput.Source {
    private final Context context;
    private final int source;
    private volatile AudioRecord active;
    private volatile boolean speechActive;
    public boolean speaking(){return speechActive;}
    VoicePcmCapture(Context context,int source){this.context=context;this.source=source;}
    public void stop(){AudioRecord r=active;if(r!=null)try{r.stop();}catch(RuntimeException ignored){}}
    public byte[] capture(BooleanSupplier finish)throws Exception{
        if(finish.getAsBoolean())return null;
        CaptureLease lease=null;RecognitionNoiseControl noise=null;AudioRecord r=null;PowerManager.WakeLock wake=null;
        try{
            lease=CaptureLease.acquire(CaptureLease.MICROPHONE,finish);
            if(finish.getAsBoolean())return null;
            r=new AudioRecord(source,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,
                Math.max(16000,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)));
            if(r.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("マイクを初期化できません");
            MicrophoneRouting.preferUsb(context,r);
            if(source==MediaRecorder.AudioSource.VOICE_RECOGNITION)noise=new RecognitionNoiseControl(r);
            PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if(pm!=null){wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"loki:voice-capture");wake.acquire(65000L);}
            active=r;r.startRecording();
            ByteArrayOutputStream pcm=new ByteArrayOutputStream();byte[] chunk=new byte[4000];
            long start=SystemClock.elapsedRealtime(),last=start;int first=-1,end=-1,hits=0;
            while(!finish.getAsBoolean()){
                int n=r.read(chunk,0,chunk.length,AudioRecord.READ_NON_BLOCKING);
                if(n<0){if(finish.getAsBoolean())break;throw new IllegalStateException("マイク読み取り失敗");}
                long now=SystemClock.elapsedRealtime();
                if(n>0){
                    MicrophoneRouting.boostAndLog(chunk,n,"VOICE_CONTINUOUS",source);
                    int count=Math.min(n,ContinuousVoiceInput.MAX_CLIP_BYTES-pcm.size());
                    int offset=pcm.size();pcm.write(chunk,0,count);
                    if(Pcm16Gain.meanAbsolute(chunk,count)>45){if(hits>=1)speechActive=true;if(first<0)first=Math.max(0,offset-8000);end=pcm.size();last=now;hits++;}
                }else Thread.sleep(15);
                if(pcm.size()>=ContinuousVoiceInput.MAX_CLIP_BYTES||VoiceCaptureTimingPolicy.stop(now-start,now-last,first>=0))break;
            }
            if(first<0||hits<2)return null;
            byte[] data=pcm.toByteArray();return Arrays.copyOfRange(data,first,Math.min(data.length,end+16000));
        }finally{
            speechActive=false;active=null;if(r!=null){try{r.stop();}catch(RuntimeException ignored){}r.release();}
            if(noise!=null)noise.close();if(lease!=null)lease.close();if(wake!=null&&wake.isHeld())wake.release();
        }
    }
}