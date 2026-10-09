package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

/** Microphone input preference only; playback capture keeps its own routing. */
final class MicrophoneRouting {
    private MicrophoneRouting() {}
    private static long lastGainLog;
    static void boostAndLog(byte[] pcm, int count, String consumer, int source) {
        int before = Pcm16Gain.meanAbsolute(pcm, count);
        int clipped = Pcm16Gain.boost3x(pcm, count);
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastGainLog >= 1000L) {
            lastGainLog = now;
            Log.i("LokiAudioCapture", consumer + " AudioSource=" + source
                    + " gain=3.0 rawMeanAbs=" + before
                    + " boostedMeanAbs=" + Pcm16Gain.meanAbsolute(pcm, count)
                    + " clippedSamples=" + clipped + " bytes=" + count);
        }
    }
    static void preferUsb(Context context, AudioRecord recorder) {
        try {
            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (manager == null) return;
            AudioDeviceInfo[] inputs = manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
            // A headset input is preferred to a generic USB audio input.
            for (int type : new int[]{AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE}) {
                for (AudioDeviceInfo input : inputs) {
                    if (input != null && input.isSource() && input.getType() == type) {
                        boolean accepted = recorder.setPreferredDevice(input);
                        Log.i("LokiAudioCapture", "USB input id=" + input.getId()
                                + " type=" + type + " preferredAccepted=" + accepted);
                        if (accepted) return;
                    }
                }
            }
            Log.i("LokiAudioCapture", "No accepted USB input; using system microphone route");
        } catch (RuntimeException unavailable) {
            Log.w("LokiAudioCapture", "USB microphone preference unavailable", unavailable);
        }
    }
}