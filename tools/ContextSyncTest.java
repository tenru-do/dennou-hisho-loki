package com.example.rokidgeminisecretary;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ContextSyncTest {
    public static void main(String[] args) throws Exception {
        String[] names = {"STEPS", "LOCATION", "WEATHER", "TOPIC"};
        Exception[] failures = {new TimeoutException("deadline"), new SecurityException("permission"),
                new IOException("HTTP 503"), new NullPointerException("null")};
        int cases = 0;
        for (int broken = 0; broken < 4; broken++) for (Exception failure : failures) {
            final int bad = broken;
            AtomicInteger ran = new AtomicInteger(), logged = new AtomicInteger(), sent = new AtomicInteger();
            ContextSyncPolicy.Reporter reporter = (name, error) -> {
                if (!names[bad].equals(name) || error != failure) throw new AssertionError("lost exception");
                logged.incrementAndGet();
            };
            for (int i = 0; i < 4; i++) {
                final int element = i;
                ContextSyncPolicy.Task<String> task = () -> {
                    ran.incrementAndGet();
                    if (element == bad) throw failure;
                    return names[element];
                };
                String value = i == 3 ? ContextSyncPolicy.topic(task, "local topic", reporter)
                        : ContextSyncPolicy.attempt(names[i], task, "", reporter);
                if (!value.isEmpty()) sent.incrementAndGet();
            }
            if (ran.get() != 4 || logged.get() != 1 || sent.get() != (bad == 3 ? 4 : 3))
                throw new AssertionError("source failure interrupted completion");
            cases++;
        }
        for (String value : new String[]{null, "", "  "})
            if (!"local".equals(ContextSyncPolicy.topic(() -> value, "local", (n,e) -> {})))
                throw new AssertionError("empty topic escaped fallback");
        CountDownLatch blocked = new CountDownLatch(1), release = new CountDownLatch(1);
        CountDownLatch healthy = new CountDownLatch(3), retry = new CountDownLatch(2);
        AtomicInteger attempts = new AtomicInteger();
        try (ContextSyncJobs jobs = new ContextSyncJobs()) {
            jobs.add("LOCATION", () -> { blocked.countDown(); release.await(); return true; },
                    10, TimeUnit.MILLISECONDS, (n,e) -> {});
            if (!blocked.await(2, TimeUnit.SECONDS)) throw new AssertionError("location not started");
            for (String name : new String[]{"STEPS", "WEATHER", "TOPIC"}) {
                java.util.concurrent.atomic.AtomicBoolean first = new java.util.concurrent.atomic.AtomicBoolean(true);
                jobs.add(name, () -> { if (first.getAndSet(false)) healthy.countDown(); return true; },
                        10, TimeUnit.MILLISECONDS, (n,e) -> {});
            }
            jobs.add("retry", () -> { retry.countDown(); attempts.incrementAndGet(); throw new IOException("503"); },
                    10, TimeUnit.MILLISECONDS, (n,e) -> {});
            if (!healthy.await(2, TimeUnit.SECONDS) || !retry.await(2, TimeUnit.SECONDS))
                throw new AssertionError("blocked/failed job prevented independent jobs or next run");
        } finally { release.countDown(); }
        System.out.println("PASS: " + cases + " fault cases, 3 empty topics, blocked-source isolation, retry after failure");
    }
}
