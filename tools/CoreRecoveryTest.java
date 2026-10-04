package com.example.rokidkeyboardbridge;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class CoreRecoveryTest {
    static int checks;
    static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        for (int count = 2; count <= 10; count++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < count; i++) text.append("文章").append(i).append("です。");
            String[] chunks = TtsSentenceBuffer.splitTopic(text.toString());
            check(String.join("", chunks).equals(text.toString()), "all sentences retained");
            for (String chunk : chunks) {
                int sentences = chunk.length() - chunk.replace("。", "").length();
                check(sentences >= 2 && sentences <= 3, "two or three sentences, count=" + count);
            }
        }
        check(TtsSentenceBuffer.splitTopic("一文です。\n\n二文です。")[0].contains("\n"), "paragraphs retained");
        check(TtsSentenceBuffer.splitTopic("").length == 0, "empty");
        check(RadioPlaybackPolicy.MIN_SPEECH_MS == 700, "700ms VAD");
        String longText = String.join("", java.util.Collections.nCopies(500, "あ")) + "。次です。";
        for (String chunk : TtsSentenceBuffer.splitTopic(longText)) check(chunk.length() <= TtsSentenceBuffer.MAX, "transport cap");
        Semaphore resource = new Semaphore(1, true);
        CaptureLease first = CaptureLease.acquire(resource, () -> false);
        AtomicBoolean waiting = new AtomicBoolean();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<CaptureLease> second = executor.submit(() -> CaptureLease.acquire(resource, () -> { waiting.set(true); return false; }));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (!waiting.get() && System.nanoTime() < deadline) Thread.yield();
            check(waiting.get() && !second.isDone(), "next capture waits for release");
            first.close(); first.close();
            CaptureLease acquired = second.get(1, TimeUnit.SECONDS);
            check(resource.availablePermits() == 0, "only one owner after repeated close");
            acquired.close(); check(resource.availablePermits() == 1, "released");
            try { CaptureLease.acquire(resource, () -> true); throw new AssertionError("cancelled capture acquired"); }
            catch (InterruptedException expected) { check(resource.availablePermits() == 1, "cancel does not leak"); }
            try (CaptureLease camera = CaptureLease.acquire(CaptureLease.CAMERA, () -> false);
                 CaptureLease audio = CaptureLease.acquire(CaptureLease.MICROPHONE, () -> false)) {
                check(true, "camera and audio independent");
            }
        } finally { first.close(); executor.shutdownNow(); }
        System.out.println("CoreRecovery PASS checks=" + checks);
    }
}
