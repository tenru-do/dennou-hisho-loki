package com.example.rokidkeyboardbridge;

/** Missing dates and clock rollback must not revive an old cache. */
public final class CacheFreshnessPolicy {
    public static final long SHARED_MEMORY_TTL_MS = 30L * 60L * 1000L;
    private CacheFreshnessPolicy() {}
    public static boolean fresh(long at, long now, long ttl) {
        return at > 0L && at <= now && ttl > 0L && now - at < ttl;
    }
}
