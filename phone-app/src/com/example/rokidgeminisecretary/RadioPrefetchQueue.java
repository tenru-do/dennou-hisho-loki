package com.example.rokidgeminisecretary;

/** One next topic; a leased topic is retained until START acknowledges delivery. */
final class RadioPrefetchQueue<T> {
    private String chain = "", currentId = "", nextId = "", leaseId = "";
    private long revision, serial, counter, work;
    private boolean active, generating, speaking;
    private T next, lease;
    synchronized boolean activate(String id, long rev, long order) {
        if (id.isEmpty() || order < serial || (order == serial && !chain.equals(id))) return false;
        if (chain.equals(id) && revision == rev && order == serial) return active;
        clear(); chain = id; revision = rev; serial = order; active = true;
        return true;
    }
    synchronized boolean matches(String id, long rev) { return active && chain.equals(id) && revision == rev; }
    synchronized String chain() { return active ? chain : ""; }
    synchronized long serial() { return serial; }
    synchronized boolean speaking() { return speaking; }
    synchronized long beginGeneration() {
        if (!active || generating || next != null || lease != null) return -1;
        generating = true; work = ++counter; return work;
    }
    synchronized boolean currentWork(long ticket) { return active && ticket == work; }
    synchronized void finishGeneration(long ticket) { if (currentWork(ticket)) generating = false; }
    synchronized boolean offer(long ticket, String topicId, T value) {
        if (!currentWork(ticket) || !generating || value == null || next != null || lease != null) return false;
        next = value; nextId = topicId; generating = false; return true;
    }
    synchronized T poll(String id, long rev) {
        if (!matches(id, rev)) return null;
        if (lease != null) return lease;
        lease = next; leaseId = nextId; next = null; nextId = "";
        return lease;
    }
    synchronized boolean start(String id, long rev, String topicId) {
        if (!matches(id, rev)) return false;
        if (topicId.equals(currentId) && !currentId.isEmpty()) return true;
        if (lease == null || !leaseId.equals(topicId)) return false;
        currentId = topicId; lease = null; leaseId = ""; speaking = true; return true;
    }
    synchronized boolean finish(String id, long rev, String topicId) {
        if (!matches(id, rev) || !currentId.equals(topicId)) return false;
        speaking = false; return true;
    }
    synchronized boolean cancel(String id, long rev) {
        if (!matches(id, rev)) return false;
        clear(); return true;
    }
    synchronized void clear() {
        // Keep the chain/serial tombstone: a delayed READY cannot revive this session.
        active = generating = speaking = false; work = ++counter;
        currentId = nextId = leaseId = ""; next = lease = null;
    }
}
