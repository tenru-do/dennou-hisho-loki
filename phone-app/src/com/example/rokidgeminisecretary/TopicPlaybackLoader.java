package com.example.rokidgeminisecretary;
import java.util.concurrent.*;

/** Reuse one in-flight build; a slow source never blocks a playback response indefinitely. */
public final class TopicPlaybackLoader<T> implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<T> inFlight;
    public T load(Callable<T> build, T fallback, long timeoutMs, ContextSyncPolicy.Reporter reporter) {
        return loadLazy(build, () -> fallback, timeoutMs, reporter);
    }
    public T loadLazy(Callable<T> build, Callable<T> fallback, long timeoutMs, ContextSyncPolicy.Reporter reporter) {
        Future<T> pending;
        synchronized (this) {
            if (inFlight == null) inFlight = executor.submit(build);
            pending = inFlight;
        }
        try {
            T result = pending.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result == null) throw new IllegalStateException("Empty TOPIC build");
            return result;
        } catch (Exception error) {
            reporter.failed("TOPIC/playback", error);
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            try { return fallback.call(); }
            catch (Exception failure) { throw new IllegalStateException("Local TOPIC unavailable", failure); }
        } finally {
            synchronized (this) { if (pending.isDone() && inFlight == pending) inFlight = null; }
        }
    }
    public void close() { executor.shutdownNow(); }
}
