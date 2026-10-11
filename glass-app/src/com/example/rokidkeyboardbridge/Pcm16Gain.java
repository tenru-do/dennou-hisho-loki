package com.example.rokidkeyboardbridge;

/** In-place little-endian PCM16 gain. Only complete samples in the valid read are touched. */
public final class Pcm16Gain {
    private Pcm16Gain() {}
    public static int boost3x(byte[] pcm, int count) {
        if (pcm == null || count <= 0) return 0;
        int end = Math.min(count, pcm.length) & ~1;
        int clipped = 0;
        for (int i = 0; i < end; i += 2) {
            int sample = (short)((pcm[i] & 255) | ((pcm[i + 1] & 255) << 8));
            int amplified = sample * 3;
            if (amplified > Short.MAX_VALUE) { amplified = Short.MAX_VALUE; clipped++; }
            else if (amplified < Short.MIN_VALUE) { amplified = Short.MIN_VALUE; clipped++; }
            pcm[i] = (byte)amplified;
            pcm[i + 1] = (byte)(amplified >> 8);
        }
        return clipped;
    }
    public static int meanAbsolute(byte[] pcm, int count) {
        if (pcm == null || count <= 1) return 0;
        int end = Math.min(count, pcm.length) & ~1;
        long total = 0;
        for (int i = 0; i < end; i += 2) {
            int sample = (short)((pcm[i] & 255) | ((pcm[i + 1] & 255) << 8));
            total += Math.abs(sample);
        }
        return (int)(total / (end / 2));
    }
}