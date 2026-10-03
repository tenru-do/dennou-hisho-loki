package com.example.rokidgeminisecretary;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class TopicPlaybackLoaderTest {
    public static void main(String[] args) throws Exception {
        AtomicInteger started = new AtomicInteger(), logged = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1), entered = new CountDownLatch(1);
        ContextSyncPolicy.Reporter reporter = (name, error) -> logged.incrementAndGet();
        try (TopicPlaybackLoader<String> loader = new TopicPlaybackLoader<>()) {
            Callable<String> slow = () -> { started.incrementAndGet(); entered.countDown(); release.await(); return "ready"; };
            if (!"local".equals(loader.load(slow, "local", 20, reporter)))
                throw new AssertionError("blocked topic did not fall back");
            if (!entered.await(1, TimeUnit.SECONDS)) throw new AssertionError("build not started");
            if (!"local".equals(loader.load(slow, "local", 20, reporter)) || started.get() != 1)
                throw new AssertionError("duplicate build while waiting");
            release.countDown();
            if (!"ready".equals(loader.load(slow, "local", 1000, reporter)))
                throw new AssertionError("result did not recover");
            if (!"local".equals(loader.load(() -> { throw new SecurityException("permission"); }, "local", 1000, reporter)))
                throw new AssertionError("permission failure stopped playback");
            if (!"local".equals(loader.load(() -> null, "local", 1000, reporter)))
                throw new AssertionError("null stopped playback");
            if (!"next".equals(loader.load(() -> "next", "local", 1000, reporter)))
                throw new AssertionError("next build failed");
        } finally { release.countDown(); }
        if (logged.get() != 4) throw new AssertionError("expected timeout/error logs");
        System.out.println("PASS: playback deadline, no duplicate build, eventual result, permission, null, next request");
    }
}
