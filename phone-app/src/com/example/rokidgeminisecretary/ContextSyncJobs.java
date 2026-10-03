package com.example.rokidgeminisecretary;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** A blocked source cannot occupy another source's execution slot. */
public final class ContextSyncJobs implements AutoCloseable {
    private final List<ScheduledExecutorService> executors = new ArrayList<>();
    public synchronized void add(String name, ContextSyncPolicy.Task<Boolean> task,
                                  long period, TimeUnit unit, ContextSyncPolicy.Reporter reporter) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executors.add(executor);
        executor.scheduleWithFixedDelay(
                () -> ContextSyncPolicy.attempt(name, task, false, reporter), 0, period, unit);
    }
    public synchronized void close() {
        for (ScheduledExecutorService executor : executors) executor.shutdownNow();
        executors.clear();
    }
}
