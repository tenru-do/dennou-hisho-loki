package com.example.rokidkeyboardbridge;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Separate camera and microphone ownership; cancelled workers cannot acquire a new resource. */
final class CaptureLease implements AutoCloseable {
    static final Semaphore MICROPHONE = new Semaphore(1, true);
    static final Semaphore CAMERA = new Semaphore(1, true);
    private Semaphore resource;
    private CaptureLease(Semaphore resource) { this.resource = resource; }
    static CaptureLease acquire(Semaphore resource, BooleanSupplier cancelled) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!cancelled.getAsBoolean() && System.nanoTime() < deadline) {
            if (resource.tryAcquire(50, TimeUnit.MILLISECONDS)) {
                if (cancelled.getAsBoolean()) { resource.release(); throw new InterruptedException("capture cancelled"); }
                return new CaptureLease(resource);
            }
        }
        throw new InterruptedException("capture busy or cancelled");
    }
    @Override public synchronized void close() {
        if (resource != null) { resource.release(); resource = null; }
    }
}
