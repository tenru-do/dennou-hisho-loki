package com.example.rokidkeyboardbridge;

/** READ preempts optional KEEP work. External cameras are never closed by Loki. */
final class CameraPriority {
    static final CameraPriority SHARED = new CameraPriority();
    private int readers;
    private long generation, quietUntil;
    private boolean externalBusy;
    synchronized ReadRequest read() { readers++; generation++; return new ReadRequest(this); }
    synchronized long keepTicket(long now) { return blocked(now) ? -1 : generation; }
    synchronized boolean yieldKeep(long ticket, long now) { return ticket < 0 || ticket != generation || blocked(now); }
    private boolean blocked(long now) { return readers > 0 || externalBusy || now < quietUntil; }
    synchronized void externalUnavailable() { externalBusy = true; generation++; }
    synchronized void externalAvailable(long now) { if(externalBusy) { externalBusy=false; defer(now,1500); } }
    synchronized void defer(long now,long delay) { generation++; quietUntil=Math.max(quietUntil,now+delay); }
    synchronized void resetExternal() { externalBusy=false; }
    private synchronized void finishRead() { if(readers<=0)throw new IllegalStateException("unbalanced camera request"); readers--; }
    static final class ReadRequest implements AutoCloseable {
        private CameraPriority owner;
        private ReadRequest(CameraPriority owner) { this.owner=owner; }
        public synchronized void close() { if(owner!=null) { owner.finishRead(); owner=null; } }
    }
}
