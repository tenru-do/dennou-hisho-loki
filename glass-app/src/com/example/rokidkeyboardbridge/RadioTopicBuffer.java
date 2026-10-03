package com.example.rokidkeyboardbridge;

/** Glass-side one-slot buffer; elapsed time is supplied by the caller. */
public final class RadioTopicBuffer<T> {
    public static final long PAUSE_MS = 1800L;
    private String chain = "", current = "", nextId = "";
    private long revision, serial, token, due, expires;
    private boolean waiting;
    private T next;
    public synchronized void arm(String id, long rev, long order) {
        clear(); chain = id; revision = rev; serial = order; waiting = true;
    }
    public synchronized void clear() { token++; chain = current = nextId = ""; next = null; waiting = false; due = expires = 0; }
    public synchronized String chain() { return chain; }
    public synchronized long revision() { return revision; }
    public synchronized long serial() { return serial; }
    public synchronized long token() { return token; }
    public synchronized String current() { return current; }
    public synchronized boolean needsTopic() { return !chain.isEmpty() && next == null; }
    public synchronized boolean offer(String id, long rev, String topicId, long expiry, long wallNow, T value) {
        if (chain.isEmpty() || !chain.equals(id) || revision != rev || topicId.isEmpty()
                || topicId.equals(current) || next != null || expiry <= wallNow || value == null) return false;
        next = value; nextId = topicId; expires = expiry; return true;
    }
    public synchronized void started(String topicId) { current = topicId; waiting = false; }
    public synchronized void finished(long elapsedNow) { waiting = true; due = elapsedNow + PAUSE_MS; }
    public synchronized boolean expired(long wallNow) { return next != null && expires <= wallNow; }
    public synchronized long remaining(long elapsedNow) { return Math.max(0, due - elapsedNow); }
    public synchronized T take(long elapsedNow) {
        if (!waiting || next == null || elapsedNow < due) return null;
        T result = next; current = nextId; next = null; nextId = ""; waiting = false; return result;
    }
}
