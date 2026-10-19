package com.example.rokidkeyboardbridge;
/** Bounded sound observations retain hesitation without becoming an unlimited recorder. */
final class ObservationAudioPolicy {
 static final long MAX_MS=12000L,MIN_MS=4000L,SILENCE_MS=VoiceCaptureTimingPolicy.SILENCE_MS;
 static final int MAX_BYTES=384000;
 private final long limit;
 ObservationAudioPolicy(){this(MAX_MS);}
 ObservationAudioPolicy(long limit){this.limit=Math.max(MIN_MS,Math.min(MAX_MS,limit));}
 private long lastSignal;private boolean heard;
 void observe(long elapsed,int level){if(level>45){heard=true;lastSignal=elapsed;}}
 boolean stop(long elapsed,int bytes){return elapsed>=limit||bytes>=Math.min(MAX_BYTES,limit*32)||(elapsed>=MIN_MS&&(!heard||elapsed-lastSignal>=SILENCE_MS));}
}
