package com.example.rokidkeyboardbridge;

import android.media.AudioRecord;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

/** Optional native suppression attached to this recording session only. */
final class RecognitionNoiseControl implements AutoCloseable {
    private NoiseSuppressor effect;
    private android.media.audiofx.AcousticEchoCanceler echo;
    RecognitionNoiseControl(AudioRecord recorder) { this(recorder, true); }
    RecognitionNoiseControl(AudioRecord recorder, boolean enableEcho) {
        if (enableEcho) {
            try {
                if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                    echo = android.media.audiofx.AcousticEchoCanceler.create(recorder.getAudioSessionId());
                    if (echo != null) echo.setEnabled(true);
                }
                Log.i("LokiAudioCapture", "echoCancellation=" + (echo != null && echo.getEnabled()));
            } catch (RuntimeException unavailable) {
                if (echo != null) { try { echo.release(); } catch (RuntimeException ignored) {} echo = null; }
                Log.i("LokiAudioCapture", "native echo cancellation unavailable");
            }
        }
        try {
            if (NoiseSuppressor.isAvailable()) {
                effect = NoiseSuppressor.create(recorder.getAudioSessionId());
                if (effect != null) effect.setEnabled(true);
            }
            Log.i("LokiAudioCapture", "VOICE_RECOGNITION noiseSuppression=" + (effect != null && effect.getEnabled()));
        } catch (RuntimeException unavailable) {
            Log.i("LokiAudioCapture", "native noise suppression unavailable");
            close();
        }
    }
    @Override public void close() {
        if (echo != null) {
            try { echo.release(); } catch (RuntimeException ignored) {}
            finally { echo = null; }
        }
        if (effect != null) {
            try { effect.release(); } catch (RuntimeException ignored) {}
            finally { effect = null; }
        }
    }
}
