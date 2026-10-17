package com.example.rokidkeyboardbridge;
/** Natural intra-sentence pauses must not terminate a spoken turn. */
final class VoiceCaptureTimingPolicy {
 static final long MAX_MS=60000L,NO_SPEECH_MS=6000L,SILENCE_MS=2200L,MIN_MS=1500L;
 static boolean stop(long elapsed,long silent,boolean heard){return elapsed>=MAX_MS||(!heard&&elapsed>=NO_SPEECH_MS)||(heard&&elapsed>=MIN_MS&&silent>=SILENCE_MS);}
 static int bridgeTimeoutMs(int bytes,int rate){return (int)Math.min(90000L,Math.max(30000L,(long)bytes*1000/Math.max(1,rate*2)+25000L));}
}
