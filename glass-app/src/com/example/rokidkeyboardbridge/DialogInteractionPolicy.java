package com.example.rokidkeyboardbridge;
/** Bounded idle lease: continued input renews it, background/dismissal releases it. */
final class DialogInteractionPolicy {
 static final long IDLE_MS=90000L;
 static boolean active(boolean foreground,long now,long until){return foreground&&now>=0&&now<until;}
}
