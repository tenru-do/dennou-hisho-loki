package com.example.rokidgeminisecretary;

/** Exception boundaries shared by production jobs and fault-injection tests. */
public final class ContextSyncPolicy {
    public interface Task<T> { T run() throws Exception; }
    public interface Reporter { void failed(String element, Exception error); }
    public static <T> T attempt(String element, Task<T> task, T fallback, Reporter log) {
        try {
            T value = task.run();
            if (value == null) throw new IllegalStateException(element + " returned null");
            return value;
        } catch (Exception error) {
            log.failed(element, error);
            return fallback;
        }
    }
    public static String topic(Task<String> task, String fallback, Reporter log) {
        return attempt("TOPIC", () -> {
            String text = task.run();
            if (text == null || text.trim().isEmpty()) throw new IllegalStateException("empty TOPIC");
            return text;
        }, fallback, log);
    }
    private ContextSyncPolicy() {}
}
