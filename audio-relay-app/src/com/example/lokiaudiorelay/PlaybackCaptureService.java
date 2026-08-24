package com.example.lokiaudiorelay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class PlaybackCaptureService extends Service {
    static final String ACTION_START = "com.example.lokiaudiorelay.START";
    static final String ACTION_STOP = "com.example.lokiaudiorelay.STOP";
    static final String EXTRA_RESULT_CODE = "result_code";
    static final String EXTRA_RESULT_DATA = "result_data";
    private static final String TAG = "LokiAudioRelay";
    private static final String CHANNEL_ID = "loki_audio_relay";
    private static final int NOTIFICATION_ID = 8876;
    private static final int SAMPLE_RATE_IN = 48000;
    private static final int SAMPLE_RATE_OUT = 16000;
    private static final int LEVEL_THRESHOLD = 90;
    private static final long NO_AUDIO_TIMEOUT_MS = 3000L;
    private static final long SILENCE_STOP_MS = 1000L;
    private static final long MAX_SEGMENT_MS = 6000L;
    private static volatile boolean runtimeActive;

    private volatile boolean captureActive;
    private volatile AudioRecord recorder;
    private MediaProjection projection;
    private MediaProjection.Callback projectionCallback;
    private Thread captureThread;
    private String relayHost = "";
    private String relayToken = "";

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCapture("停止しました");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent == null || !ACTION_START.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startProjectionForeground("再生音声を準備中");
        relayHost = getPreferences().getString(MainActivity.KEY_RELAY_HOST, "").trim();
        relayToken = getPreferences().getString(MainActivity.KEY_RELAY_TOKEN, "").trim();
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (relayHost.length() == 0 || relayToken.length() < 16
                || resultCode == 0 || resultData == null) {
            stopCapture("接続設定または音声共有許可がありません");
            stopSelf();
            return START_NOT_STICKY;
        }
        startCapture(resultCode, resultData);
        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent resultData) {
        stopCaptureInternal(false);
        try {
            MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            if (manager == null) throw new IllegalStateException("MediaProjection unavailable");
            projection = manager.getMediaProjection(resultCode, resultData);
            final MediaProjection current = projection;
            projectionCallback = new MediaProjection.Callback() {
                @Override public void onStop() {
                    if (projection == current) {
                        stopCapture("音声共有が終了しました");
                        stopSelf();
                    }
                }
            };
            projection.registerCallback(projectionCallback, new Handler(Looper.getMainLooper()));
            captureActive = true;
            updateState(true, "再生音声 ON / 音声待ち");
            sendSourceStateAsync(true);
            captureThread = new Thread(new Runnable() {
                @Override public void run() {
                    runCaptureLoop();
                }
            }, "PlaybackCapture");
            captureThread.start();
        } catch (Exception error) {
            Log.w(TAG, "start capture failed", error);
            stopCapture("開始失敗: " + error.getClass().getSimpleName());
            stopSelf();
        }
    }

    private void runCaptureLoop() {
        try {
            recorder = createRecorder();
            recorder.startRecording();
            long lastHeartbeat = 0L;
            while (captureActive) {
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat >= 15000L) {
                    sendSourceState(true);
                    lastHeartbeat = now;
                }
                byte[] pcm = recordSegment(recorder);
                if (!captureActive) break;
                if (pcm == null || pcm.length < 16000) {
                    updateState(true, "再生音声 ON / 音声待ち");
                    continue;
                }
                updateState(true, "再生音声を文字化中");
                try {
                    String transcript = sendForRecognition(pcm);
                    updateState(true, transcript.length() == 0
                            ? "再生音声 ON / 聞き取りなし"
                            : "グラスへ送信済み（" + transcript.length() + "文字）");
                } catch (Exception error) {
                    Log.w(TAG, "relay recognition failed", error);
                    updateState(true, "Galaxy通信待ち: " + shortError(error));
                    Thread.sleep(2500L);
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            Log.w(TAG, "capture loop failed", error);
            updateState(false, "取得失敗: " + shortError(error));
        } finally {
            releaseRecorder();
            sendSourceState(false);
            captureActive = false;
            getPreferences().edit().putBoolean(MainActivity.KEY_CAPTURE_ACTIVE, false).apply();
        }
    }

    private AudioRecord createRecorder() throws Exception {
        if (projection == null) throw new IllegalStateException("projection missing");
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioPlaybackCaptureConfiguration configuration =
                new AudioPlaybackCaptureConfiguration.Builder(projection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build();
        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE_IN)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build();
        AudioRecord result = new AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(minBuffer * 2, 65536))
                .setAudioPlaybackCaptureConfig(configuration)
                .build();
        if (result.getState() != AudioRecord.STATE_INITIALIZED) {
            result.release();
            throw new IllegalStateException("AudioRecord init failed");
        }
        return result;
    }

    private byte[] recordSegment(AudioRecord audioRecord) throws Exception {
        short[] input = new short[4800];
        ByteArrayOutputStream output = new ByteArrayOutputStream(288000);
        long started = System.currentTimeMillis();
        long lastVoiceAt = started;
        int voiceHits = 0;
        int maxLevel = 0;
        while (captureActive) {
            int read = audioRecord.read(input, 0, input.length, AudioRecord.READ_BLOCKING);
            if (read < 0) throw new IllegalStateException("AudioRecord read " + read);
            if (read == 0) continue;
            int level = downmixAndWrite(input, read, output);
            if (level > maxLevel) maxLevel = level;
            long now = System.currentTimeMillis();
            if (level >= LEVEL_THRESHOLD) {
                voiceHits++;
                lastVoiceAt = now;
            }
            if (voiceHits == 0 && now - started >= NO_AUDIO_TIMEOUT_MS) break;
            if (voiceHits >= 3 && now - lastVoiceAt >= SILENCE_STOP_MS) break;
            if (now - started >= MAX_SEGMENT_MS) break;
        }
        Log.i(TAG, "relay segment ms=" + (System.currentTimeMillis() - started)
                + " bytes=" + output.size() + " peak=" + maxLevel
                + " voiceHits=" + voiceHits);
        if (voiceHits < 3 || maxLevel < LEVEL_THRESHOLD) return null;
        return output.toByteArray();
    }

    private int downmixAndWrite(short[] input, int count, ByteArrayOutputStream output) {
        int max = 0;
        for (int index = 0; index + 1 < count; index += 6) {
            int mono = (input[index] + input[index + 1]) / 2;
            int absolute = Math.abs(mono);
            if (absolute > max) max = absolute;
            output.write(mono & 255);
            output.write((mono >> 8) & 255);
        }
        return max;
    }

    private String sendForRecognition(byte[] pcm) throws Exception {
        JSONObject body = new JSONObject();
        body.put("sampleRate", SAMPLE_RATE_OUT);
        body.put("pcm", Base64.encodeToString(pcm, Base64.NO_WRAP));
        body.put("ambientRelay", true);
        body.put("source", "Bluetooth");
        JSONObject response = postJson("stt", body);
        if (!response.optBoolean("ok", false)) {
            throw new IllegalStateException(response.optString("error", "speech failed"));
        }
        return response.optString("transcript", "").trim();
    }

    private void sendSourceStateAsync(final boolean active) {
        new Thread(new Runnable() {
            @Override public void run() {
                sendSourceState(active);
            }
        }, "RelayHeartbeat").start();
    }

    private void sendSourceState(boolean active) {
        try {
            JSONObject body = new JSONObject();
            body.put("active", active);
            body.put("source", "Bluetooth");
            postJson("ambient_source_state", body);
        } catch (Exception error) {
            Log.w(TAG, "source state failed", error);
        }
    }

    private JSONObject postJson(String path, JSONObject body) throws Exception {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://" + relayHost + ":" + MainActivity.PORT + "/" + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(1500);
        // Galaxy may retry once with its second recognizer when the first
        // on-device/online attempt times out.
        connection.setReadTimeout(50000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-Roki-Token", relayToken);
        connection.setFixedLengthStreamingMode(bytes.length);
        OutputStream out = connection.getOutputStream();
        out.write(bytes);
        out.close();
        int code = connection.getResponseCode();
        String response = readAll(code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }
        return new JSONObject(response.length() == 0 ? "{}" : response);
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }

    private void startProjectionForeground(String text) {
        Notification notification = buildNotification(text);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "ロキ Audio Relay", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Bluetooth送信前の再生音声をロキへ中継します。");
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("ロキ Audio Relay")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void updateState(boolean active, String status) {
        runtimeActive = active;
        getPreferences().edit()
                .putBoolean(MainActivity.KEY_CAPTURE_ACTIVE, active)
                .putString(MainActivity.KEY_CAPTURE_STATUS, status == null ? "" : status)
                .apply();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && active) manager.notify(NOTIFICATION_ID, buildNotification(status));
    }

    private void stopCapture(String status) {
        stopCaptureInternal(true);
        updateState(false, status);
    }

    private void stopCaptureInternal(boolean notifyRemote) {
        captureActive = false;
        Thread thread = captureThread;
        captureThread = null;
        if (thread != null) thread.interrupt();
        releaseRecorder();
        MediaProjection currentProjection = projection;
        MediaProjection.Callback callback = projectionCallback;
        projection = null;
        projectionCallback = null;
        if (currentProjection != null) {
            if (callback != null) {
                try { currentProjection.unregisterCallback(callback); } catch (Exception ignored) {}
            }
            try { currentProjection.stop(); } catch (Exception ignored) {}
        }
        if (notifyRemote) sendSourceStateAsync(false);
    }

    private void releaseRecorder() {
        AudioRecord current = recorder;
        recorder = null;
        if (current == null) return;
        try { current.stop(); } catch (Exception ignored) {}
        try { current.release(); } catch (Exception ignored) {}
    }

    private String shortError(Exception error) {
        String text = error == null || error.getMessage() == null
                ? "unknown" : error.getMessage().replace('\n', ' ').trim();
        return text.length() <= 80 ? text : text.substring(0, 80);
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
    }

    static boolean isActive(Context context) {
        return runtimeActive && context.getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .getBoolean(MainActivity.KEY_CAPTURE_ACTIVE, false);
    }

    @Override public void onDestroy() {
        stopCaptureInternal(true);
        updateState(false, "停止しました");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
