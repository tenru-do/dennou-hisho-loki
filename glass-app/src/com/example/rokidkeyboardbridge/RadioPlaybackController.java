package com.example.rokidkeyboardbridge;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;

/** Arms the microphone before using Loki's existing Rokid voice transport. */
final class RadioPlaybackController {
    interface VoiceTransport {
        void speak(String text) throws Exception;
        void stop() throws Exception;
    }
    interface Listener {
        void onStarted();
        void onInterrupt();
        void onSpeech(byte[] pcm);
        void onFinished();
        void onError();
    }
    private final android.content.Context context;
    RadioPlaybackController(android.content.Context context) { this.context = context.getApplicationContext(); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private volatile boolean speaking;
    private volatile boolean capturing;
    private VoiceTransport voice;
    private AmbientTap ambientTap;
    private static final class AmbientTap {
        final ByteArrayOutputStream pcm = new ByteArrayOutputStream(ObservationAudioPolicy.MAX_BYTES);
        final ObservationAudioPolicy timing=new ObservationAudioPolicy();
        final long started=SystemClock.elapsedRealtime();
        boolean finished, failed;
    }
    synchronized boolean isSpeaking() { return speaking && !capturing; }
    byte[] captureAmbientPcm(java.util.function.BooleanSupplier cancelled) throws Exception {
        final AmbientTap tap = new AmbientTap();
        final int token;
        synchronized (this) {
            if (!speaking || capturing || ambientTap != null) throw new IllegalStateException("radio microphone unavailable");
            token = generation; ambientTap = tap;
        }
        try {
            long until = SystemClock.elapsedRealtime() + ObservationAudioPolicy.MAX_MS + 1500L;
            while (!cancelled.getAsBoolean() && SystemClock.elapsedRealtime() < until) {
                synchronized (this) {
                    if (token != generation || tap.failed || capturing) throw new InterruptedException("radio observation cancelled");
                    if (tap.finished) return tap.pcm.toByteArray();
                }
                Thread.sleep(10L);
            }
            throw new InterruptedException("radio observation incomplete");
        } finally { synchronized (this) { if (ambientTap == tap) ambientTap = null; } }
    }
    private void cancelAmbientTap() {
        if (ambientTap != null) { ambientTap.failed = true; ambientTap = null; }
    }
    synchronized void stop() {
        generation++;
        cancelAmbientTap();
        boolean wasSpeaking = speaking;
        speaking = false;
        capturing = false;
        if (wasSpeaking && voice != null) {
            try { voice.stop(); }
            catch (Exception error) { android.util.Log.w("LokiRadioPlayback", "Rokid stop command failed", error); }
        }
        voice = null;
    }
    synchronized boolean active() { return speaking || capturing; }
    synchronized void play(final String text, final long durationMs,
                           final VoiceTransport transport, final Listener listener) {
        play(new String[]{text}, new long[]{durationMs}, transport, listener);
    }
    synchronized void play(final String[] chunks, final long[] durations,
                           final VoiceTransport transport, final Listener listener) {
        if (chunks.length == 0 || chunks.length != durations.length) throw new IllegalArgumentException("empty speech");
        stop();
        final int token = generation;
        voice = transport;
        speaking = true;
        new Thread(new Runnable() {
            public void run() { captureAndSpeak(chunks, durations, token, transport, listener); }
        }, "RadioBargeIn").start();
    }
    private void captureAndSpeak(String[] chunks, long[] durations, final int token,
                                 VoiceTransport transport, final Listener listener) {
        AudioRecord recorder = null;
        if(MeetingRecorderService.active!=null){
            try {
                for(int i=0;i<chunks.length&&token==generation;i++){
                    MeetingRecorderService meeting=MeetingRecorderService.active;if(meeting!=null)meeting.assistantSpeech(durations[i]);
                    transport.speak(chunks[i]);if(i==0)main.post(()->{if(token==generation)listener.onStarted();});
                    long until=SystemClock.elapsedRealtime()+durations[i];while(token==generation&&SystemClock.elapsedRealtime()<until)Thread.sleep(30);
                }
                synchronized(this){if(token!=generation)return;transport.stop();speaking=false;voice=null;}
                main.post(()->{if(token==generation)listener.onFinished();});
            }catch(Exception error){main.post(()->{if(token==generation)listener.onError();});}
            return;
        }
        AcousticEchoCanceler echo = null;
        RecognitionNoiseControl noiseControl = null;
        CaptureLease lease = null;
        try {
            lease = CaptureLease.acquire(CaptureLease.MICROPHONE, () -> token != generation);
            int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) throw new IllegalStateException("microphone unavailable");
            recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, 6400));
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("microphone unavailable");
            MicrophoneRouting.preferUsb(context, recorder);
            // Keep DEFAULT vendor processing; avoid extra suppression of quiet PCM.
            if (AcousticEchoCanceler.isAvailable()) {
                echo = AcousticEchoCanceler.create(recorder.getAudioSessionId());
                if (echo != null) echo.setEnabled(true);
            }
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException("microphone inactive");
            byte[] buffer = new byte[640];
            int noise = 0, frames = 0;
            long calibrationEnd = SystemClock.elapsedRealtime() + 400L;
            while (SystemClock.elapsedRealtime() < calibrationEnd && token == generation) {
                int n = recorder.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
                if (n < 0) throw new IllegalStateException("microphone read failed");
                if (n > 0) { MicrophoneRouting.boostAndLog(buffer, n, "RADIO", MediaRecorder.AudioSource.DEFAULT); noise += RadioPlaybackPolicy.level(buffer, n); frames++; }
                else Thread.sleep(5);
            }
            int threshold = RadioPlaybackPolicy.calibratedThreshold(frames == 0 ? 0 : noise / frames);
            android.util.Log.i("LokiRadioPlayback","VAD threshold="+threshold+" minSpeechMs=700 maxGapMs=80");
            synchronized (this) {
                if (token != generation) return;
                transport.speak(chunks[0]);
                main.post(() -> { if (token == generation) listener.onStarted(); });
                android.util.Log.i("LokiRadioPlayback", "Rokid voice started; microphone armed; echo="
                        + (echo != null && echo.getEnabled()));
            }
            // Rokid's phone TTS transport exposes no completion callback to this app.
            // Reuse the conservative duration estimate used by normal Loki answers.
            int chunkIndex = 0;
            long playbackUntil = SystemClock.elapsedRealtime() + durations[0];
            ByteArrayOutputStream lead = new ByteArrayOutputStream(), speech = new ByteArrayOutputStream();
            long detectedAt = 0, lastVoice = 0;
            RadioPlaybackPolicy detector = new RadioPlaybackPolicy(threshold);
            while (token == generation && (speaking || capturing)) {
                long now = SystemClock.elapsedRealtime();
                if (!capturing && now >= playbackUntil) {
                    synchronized (this) {
                        if (token != generation) return;
                        if (chunkIndex + 1 < chunks.length) {
                            transport.speak(chunks[++chunkIndex]);
                            playbackUntil = SystemClock.elapsedRealtime() + durations[chunkIndex];
                            continue;
                        }
                        // End the radio utterance as well as its monitoring window.
                        // This avoids leaving late playback without a microphone.
                        transport.stop();
                        cancelAmbientTap();
                        speaking = false;
                        voice = null;
                    }
                    main.post(new Runnable() { public void run() { if (token == generation) listener.onFinished(); } });
                    return;
                }
                int n = recorder.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
                if (n < 0) throw new IllegalStateException("microphone read failed");
                if (n == 0) { Thread.sleep(5); continue; }
                MicrophoneRouting.boostAndLog(buffer, n, "RADIO", MediaRecorder.AudioSource.DEFAULT);
                boolean heardVoice = RadioPlaybackPolicy.level(buffer, n) > threshold;
                if (!capturing) {
                    synchronized (this) {
                        if (ambientTap != null && token == generation) {
                            AmbientTap tap = ambientTap;
                            tap.pcm.write(buffer, 0, Math.min(n, ObservationAudioPolicy.MAX_BYTES - tap.pcm.size()));
                            tap.timing.observe(SystemClock.elapsedRealtime()-tap.started,Pcm16Gain.meanAbsolute(buffer,n));
                            if (tap.timing.stop(SystemClock.elapsedRealtime()-tap.started,tap.pcm.size())) { tap.finished = true; ambientTap = null; }
                        }
                    }
                    lead.write(buffer, 0, n);
                    if (lead.size() > 51200) {
                        byte[] old = lead.toByteArray(); lead.reset(); lead.write(old, old.length - 48000, 48000);
                    }
                    if (detector.speechDetected(buffer, n)) {
                        synchronized (this) {
                            if (token != generation) return;
                            // The same stop command is used by Rokid's own speech capability.
                            transport.stop();
                            cancelAmbientTap();
                            speaking = false; capturing = true;
                            android.util.Log.i("LokiRadioPlayback", "speech confirmed; voicedMs="+detector.voicedMs()+" threshold="+threshold+"; Rokid stop sent");
                        }
                        detectedAt = lastVoice = now;
                        speech.write(lead.toByteArray());
                        main.post(new Runnable() { public void run() { if (token == generation) listener.onInterrupt(); } });
                    }
                } else {
                    speech.write(buffer, 0, n);
                    if (heardVoice) lastVoice = now;
                    if (now - lastVoice > 1000L || now - detectedAt > 15000L) break;
                }
            }
            synchronized (this) {
                if (token == generation && capturing) {
                    final byte[] pcm = speech.toByteArray();
                    capturing = false;
                    voice = null;
                    main.post(new Runnable() { public void run() { if (token == generation) listener.onSpeech(pcm); } });
                }
            }
        } catch (Exception error) {
            android.util.Log.w("LokiRadioPlayback", "Rokid voice/microphone failed: " + error.getClass().getSimpleName());
            synchronized (this) {
                if (token == generation) {
                    stop();
                    final int stoppedToken = generation;
                    main.post(new Runnable() { public void run() { if (stoppedToken == generation) listener.onError(); } });
                }
            }
        } finally {
            try {
                if (noiseControl != null) noiseControl.close();
                try { if (echo != null) echo.release(); }
                finally { if (recorder != null) { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); } }
            } finally { if (lease != null) lease.close(); }
        }
    }
}
