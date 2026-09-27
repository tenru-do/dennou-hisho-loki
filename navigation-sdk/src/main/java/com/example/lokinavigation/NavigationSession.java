package com.example.lokinavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Single-owner, single-thread controller. The host must retain it across UI rotation. */
public final class NavigationSession<T> {
    public enum State { STOPPED, REQUESTING, ACTIVE, STOPPING, FAILED }
    public enum StartResult { STARTED, BUSY, DISABLED, INVALID, LIMIT, ERROR }
    public interface Gate { boolean enabled(); }
    public interface Quota { boolean reserve(int destinations); }
    public interface Completion { void complete(boolean routeReady); }
    public interface Engine<T> {
        void request(List<T> destinations, Completion completion);
        boolean startGuidanceAndFeed();
        void stopAndClear();
    }

    private final Gate gate;
    private final Quota quota;
    private final Engine<T> engine;
    private final Thread owner = Thread.currentThread();
    private State state = State.STOPPED;
    private boolean pending;
    private boolean cancelled;
    private long sequence;

    public NavigationSession(Gate gate, Quota quota, Engine<T> engine) {
        this.gate = gate;
        this.quota = quota;
        this.engine = engine;
    }

    private void checkThread() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Use the session owner thread");
    }

    public State state() { checkThread(); return state; }
    public boolean hasPendingRequest() { checkThread(); return pending; }

    public StartResult start(List<T> destinations) {
        checkThread();
        if (pending || state == State.ACTIVE) return StartResult.BUSY;
        if (!gate.enabled()) return StartResult.DISABLED;
        if (destinations == null || destinations.isEmpty() || destinations.size() > DestinationQuota.DAILY_LIMIT)
            return StartResult.INVALID;
        for (T destination : destinations) if (destination == null) return StartResult.INVALID;
        final List<T> request = Collections.unmodifiableList(new ArrayList<>(destinations));
        // Reserve exactly once BEFORE calling the SDK, including every waypoint.
        try {
            if (!quota.reserve(request.size())) return StartResult.LIMIT;
        } catch (RuntimeException storageFailure) {
            return StartResult.ERROR;
        }
        final long ticket = ++sequence;
        pending = true;
        cancelled = false;
        state = State.REQUESTING;
        try {
            engine.stopAndClear();
            engine.request(request, new Completion() {
                @Override public void complete(boolean ready) { finish(ticket, ready); }
            });
            return StartResult.STARTED;
        } catch (RuntimeException failure) {
            pending = false;
            state = State.FAILED;
            clearSafely();
            return StartResult.ERROR;
        }
    }

    private void finish(long ticket, boolean ready) {
        checkThread();
        if (!pending || ticket != sequence) return;
        pending = false;
        if (cancelled || !gate.enabled()) {
            state = State.STOPPED;
            clearSafely();
            return;
        }
        try {
            if (ready && engine.startGuidanceAndFeed()) { state = State.ACTIVE; return; }
        } catch (RuntimeException failure) { /* Fail closed, including receiver registration failures. */ }
        state = State.FAILED;
        clearSafely();
    }

    public void stop() {
        checkThread();
        cancelled = true;
        // Do not issue a new destination while an old request can still alter the SDK route.
        state = pending ? State.STOPPING : State.STOPPED;
        clearSafely();
    }

    private void clearSafely() {
        try { engine.stopAndClear(); }
        catch (RuntimeException failure) { state = State.FAILED; }
    }
}
