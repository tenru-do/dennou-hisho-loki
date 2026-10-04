package com.example.rokidkeyboardbridge;

import android.media.AudioRecord;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

/** Optional native suppression attached to this recording session only. */
final class RecognitionNoiseControl implements AutoCloseable {
    private NoiseSuppressor effect;
    RecognitionNoiseControl(AudioRecord recorder) {
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
        if (effect != null) {
            try { effect.release(); } catch (RuntimeException ignored) {}
            finally { effect = null; }
        }
    }
}
