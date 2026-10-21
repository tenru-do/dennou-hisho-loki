package com.example.rokidkeyboardbridge;
/** Local spacing only; server error backoff remains authoritative. */
public final class ConversationPacingPolicy {
 public static final long INTERVAL_MS=10000L;
 public static long successAt(long now,long started){return Math.max(now,started+INTERVAL_MS);}
 private ConversationPacingPolicy(){}
}
