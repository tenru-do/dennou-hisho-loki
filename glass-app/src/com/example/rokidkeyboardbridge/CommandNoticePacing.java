package com.example.rokidkeyboardbridge;
/** Bound idle/held-queue work without changing the three-second command fallback. */
final class CommandNoticePacing {
    static long delayMs(boolean success,boolean queued,int failures){
        if(success)return queued?3000L:1000L;
        return Math.min(30000L,1000L << Math.min(5,Math.max(0,failures-1)));
    }
    private CommandNoticePacing(){}
}
