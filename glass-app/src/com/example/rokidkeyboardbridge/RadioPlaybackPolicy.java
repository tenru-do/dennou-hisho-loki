package com.example.rokidkeyboardbridge;

/** Delivery and VAD rules independent of Android, including stale settings protection. */
public final class RadioPlaybackPolicy {
    public static final int MIN_LEVEL=360, SAMPLE_RATE=16000, MIN_SPEECH_MS=700, MAX_GAP_MS=80;
    private int voicedSamples, gapSamples;
    private final int threshold;
    public RadioPlaybackPolicy() { this(MIN_LEVEL); }
    public RadioPlaybackPolicy(int threshold) { this.threshold = Math.max(MIN_LEVEL, threshold); }
    public static int calibratedThreshold(int noise) { return (int)Math.min(24000L,Math.max(MIN_LEVEL,Math.max(0L,noise)*4L+120L)); }
    public int threshold() { return threshold; }
    public int voicedMs() { return voicedSamples*1000/SAMPLE_RATE; }
    public void reset() { voicedSamples=gapSamples=0; }
    public static boolean canPlay(boolean enabled, long currentRevision, long topicRevision,
                                  long expiresAt, long now, boolean busy) {
        return enabled && !busy && currentRevision == topicRevision && expiresAt > now;
    }
    public boolean speechDetected(byte[] pcm, int length) {
        if (pcm==null || length < 2) return false;
        int samples=Math.min(length,pcm.length)/2;
        if(level(pcm,length)>threshold) { voicedSamples+=samples;gapSamples=0; }
        else {gapSamples+=samples;if(gapSamples>SAMPLE_RATE*MAX_GAP_MS/1000)reset();}
        return voicedSamples>=SAMPLE_RATE*MIN_SPEECH_MS/1000 && gapSamples==0;
    }
    public static int level(byte[] pcm, int length) {
        long sum = 0;
        int samples = pcm==null?0:Math.max(0,Math.min(length, pcm.length)) / 2;
        for (int i = 0; i < samples * 2; i += 2)
            sum += Math.abs((int)(short)((pcm[i] & 255) | (pcm[i + 1] << 8)));
        return samples > 0 ? (int)(sum / samples) : 0;
    }
    public static boolean voice(byte[] pcm, int length) {
        return level(pcm, length) > MIN_LEVEL;
    }
}
