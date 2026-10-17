package com.example.rokidkeyboardbridge;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.view.View;
import android.view.ViewGroup;

/** Brief, non-interactive confirmation of a successfully saved KEEP photo. */
final class KeepCaptureFeedback {
    static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()) return;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View dot = new View(activity) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            protected void onDraw(Canvas canvas) {
                paint.setColor(Color.WHITE);
                float density = getResources().getDisplayMetrics().density;
                canvas.drawCircle(getWidth() - 12 * density, 12 * density, 3 * density, paint);
            }
        };
        dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        dot.setClickable(false);
        dot.setFocusable(false);
        decor.getOverlay().add(dot);
        dot.layout(0, 0, decor.getWidth(), decor.getHeight());
        dot.postDelayed(() -> decor.getOverlay().remove(dot), 350L);
        new Thread(KeepCaptureFeedback::click, "KeepShutterFeedback").start();
    }

    private static void click() {
        AudioTrack track = null;
        try {
            // Two short, quiet mechanical clicks; no microphone or audio focus changes.
            short[] pcm = new short[1440]; // 60 ms at 24 kHz
            int noise = 17;
            for (int i = 0; i < pcm.length; i++) {
                int at = i < 480 ? i : i - 720;
                if (at < 0 || at >= 240) continue;
                noise = noise * 1664525 + 1013904223;
                double envelope = Math.exp(-at / 45.0);
                pcm[i] = (short) (((noise >>> 16) - 32768) * envelope * 0.055);
            }
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(24000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length * 2).build();
            track.write(pcm, 0, pcm.length);
            track.play();
            Thread.sleep(100L);
        } catch (Exception error) {
            android.util.Log.w("LokiKeepFeedback", "Shutter feedback unavailable", error);
        } finally {
            if (track != null) track.release();
        }
    }
}
