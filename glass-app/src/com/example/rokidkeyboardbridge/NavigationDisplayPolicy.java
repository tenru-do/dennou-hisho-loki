package com.example.rokidkeyboardbridge;

/** Hold guidance only during a fresh, still-active session's missing-step update. */
final class NavigationDisplayPolicy {
    static boolean hold(boolean active, boolean fresh, boolean suppressed,
                        boolean previousActive, boolean currentSignal,
                        long lastSignalAt, long now, long holdMillis) {
        return active && fresh && !suppressed && previousActive && !currentSignal
                && lastSignalAt > 0 && now >= lastSignalAt && now - lastSignalAt <= holdMillis;
    }
}
