package com.example.rokidkeyboardbridge;
/** Reconnection is one state transition, never a replay of queued actions. */
public final class NavigationLinkRecovery {
 private boolean failed;private long lastSuccess;
 public synchronized boolean fail(){boolean first=!failed;failed=true;return first;}
 public synchronized boolean accept(long now){boolean recovered=failed;failed=false;lastSuccess=now;return recovered;}
 public synchronized long lastSuccess(){return lastSuccess;}
}
