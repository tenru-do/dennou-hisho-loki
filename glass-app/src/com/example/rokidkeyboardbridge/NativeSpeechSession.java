package com.example.rokidkeyboardbridge;
/** A stop belongs to one live Loki utterance, never to an idle media session. */
final class NativeSpeechSession {
 private long until;
 synchronized boolean active(long now){return until>0&&now<until;}
 synchronized void sent(long now,long duration){until=now+Math.max(1000,Math.min(600000,duration));}
 synchronized boolean takeStop(long now){boolean stop=active(now);until=0;return stop;}
}
