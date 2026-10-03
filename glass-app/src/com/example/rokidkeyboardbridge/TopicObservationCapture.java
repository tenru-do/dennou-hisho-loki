package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Short observations between utterances; never saves recordings or photos to disk. */
final class TopicObservationCapture {
    interface AudioProvider { byte[] read(java.util.function.BooleanSupplier cancelled) throws Exception; }
    static JSONObject capture(Context context, JSONObject request, BooleanSupplier cancelled) throws Exception {
        return capture(context, request, cancelled, null);
    }
    static JSONObject capture(Context context, JSONObject request, BooleanSupplier cancelled, AudioProvider sharedAudio) throws Exception {
        JSONObject result = new JSONObject().put("requestId", request.getString("requestId"))
                .put("revision", request.getLong("revision"));
        String mode = request.optString("mode");
        if ("sound".equals(mode) || "both".equals(mode)) {
            try {
                if (context.checkSelfPermission("android.permission.RECORD_AUDIO") != 0) throw new IllegalStateException();
                result.put("audio", Base64.encodeToString(sharedAudio == null ? audio(cancelled) : wav(sharedAudio.read(cancelled)), Base64.NO_WRAP));
                if (sharedAudio != null) result.put("audioDuringAssistantSpeech", true);
            } catch (Exception error) { result.put("audioUnavailable", true); }
        }
        if (!cancelled.getAsBoolean() && ("vision".equals(mode) || "both".equals(mode))) {
            try {
                if (context.checkSelfPermission("android.permission.CAMERA") != 0) throw new IllegalStateException();
                result.put("image", Base64.encodeToString(photo(cancelled), Base64.NO_WRAP));
            } catch (Exception error) { result.put("imageUnavailable", true); }
        }
        if (cancelled.getAsBoolean()) throw new InterruptedException("observation cancelled");
        return result;
    }
    private static byte[] audio(BooleanSupplier cancelled) throws Exception {
        int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) throw new IllegalStateException("microphone unavailable");
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, 6400));
        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException();
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException();
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] frame = new byte[640];
            long until = SystemClock.elapsedRealtime() + 4000L;
            while (!cancelled.getAsBoolean() && SystemClock.elapsedRealtime() < until && pcm.size() < 128000) {
                int count = recorder.read(frame, 0, Math.min(frame.length, 128000 - pcm.size()), AudioRecord.READ_NON_BLOCKING);
                if (count < 0) throw new IllegalStateException();
                if (count > 0) pcm.write(frame, 0, count); else Thread.sleep(5L);
            }
            if (cancelled.getAsBoolean() || pcm.size() < 3200) throw new InterruptedException();
            return wav(pcm.toByteArray());
        } finally { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); }
    }
    private static byte[] wav(byte[] data) {
            ByteBuffer wav = ByteBuffer.allocate(44 + data.length).order(ByteOrder.LITTLE_ENDIAN);
            wav.put(new byte[]{'R','I','F','F'}).putInt(36 + data.length).put(new byte[]{'W','A','V','E','f','m','t',' '});
            wav.putInt(16).putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000);
            wav.putShort((short) 2).putShort((short) 16).put(new byte[]{'d','a','t','a'}).putInt(data.length).put(data);
            return wav.array();
    }
    @SuppressWarnings("deprecation")
    private static byte[] photo(BooleanSupplier cancelled) throws Exception {
        HandlerThread thread = new HandlerThread("TopicCamera"); thread.start();
        Handler cameraHandler = new Handler(thread.getLooper());
        CountDownLatch done = new CountDownLatch(1);
        Camera[] camera = new Camera[1]; SurfaceTexture[] surface = new SurfaceTexture[1]; byte[][] jpeg = new byte[1][];
        try {
            cameraHandler.post(() -> {
                try {
                    if (cancelled.getAsBoolean()) { done.countDown(); return; }
                    camera[0] = Camera.open();
                    Camera.Parameters parameters = camera[0].getParameters();
                    Camera.Size chosen = null;
                    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                        if (size.width <= 1280 && (chosen == null || size.width * size.height > chosen.width * chosen.height)) chosen = size;
                    }
                    if (chosen == null) for (Camera.Size size : parameters.getSupportedPictureSizes())
                        if (chosen == null || size.width * size.height < chosen.width * chosen.height) chosen = size;
                    if (chosen != null) parameters.setPictureSize(chosen.width, chosen.height);
                    parameters.setJpegQuality(70); camera[0].setParameters(parameters);
                    surface[0] = new SurfaceTexture(0); camera[0].setPreviewTexture(surface[0]); camera[0].startPreview();
                    cameraHandler.postDelayed(() -> {
                        try {
                            if (cancelled.getAsBoolean()) { done.countDown(); return; }
                            camera[0].takePicture(null, null, (bytes, source) -> { jpeg[0] = bytes; done.countDown(); });
                        } catch (Exception error) { done.countDown(); }
                    }, 600L);
                } catch (Exception error) { done.countDown(); }
            });
            long until = SystemClock.elapsedRealtime() + 6000L;
            while (!cancelled.getAsBoolean() && SystemClock.elapsedRealtime() < until && !done.await(100, TimeUnit.MILLISECONDS)) {}
            if (cancelled.getAsBoolean() || jpeg[0] == null) throw new IllegalStateException("camera unavailable");
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 640);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length, options);
            if (bitmap == null) throw new IllegalStateException();
            try {
                Camera.CameraInfo info = new Camera.CameraInfo(); Camera.getCameraInfo(0, info);
                android.graphics.Matrix rotation = new android.graphics.Matrix(); rotation.postRotate(info.orientation);
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try { oriented.compress(Bitmap.CompressFormat.JPEG, 65, out); }
                finally { if (oriented != bitmap) oriented.recycle(); }
                if (out.size() > 250000) throw new IllegalStateException("image too large");
                return out.toByteArray();
            } finally { bitmap.recycle(); }
        } finally {
            CountDownLatch released = new CountDownLatch(1);
            cameraHandler.post(() -> {
                try { if (camera[0] != null) camera[0].release(); }
                finally { if (surface[0] != null) surface[0].release(); released.countDown(); }
            });
            released.await(2, TimeUnit.SECONDS); thread.quitSafely();
        }
    }
}
