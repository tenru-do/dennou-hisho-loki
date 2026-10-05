package com.example.rokidkeyboardbridge;

/** A user conversation suspends a continuous session, without switching it off. */
public final class RadioResumePolicy {
    public static final long PAUSE_MS = 2500L;
    private boolean suspended;
    private long revision, idleSince = -1L;
    public void suspend(long settingsRevision) {
        suspended = true; revision = settingsRevision; idleSince = -1L;
    }
    public void clear() { suspended = false; idleSince = -1L; }
    public boolean suspended() { return suspended; }
    public boolean ready(boolean enabled, String mode, long settingsRevision, boolean busy, long now) {
        if (!enabled || "scheduled".equals(mode) || settingsRevision != revision) {
            clear(); return false;
        }
        if (!suspended) return true;
        if (busy) { idleSince = -1L; return false; }
        if (idleSince < 0) idleSince = now;
        if (now - idleSince < PAUSE_MS) return false;
        clear(); return true;
    }
}
