package com.example.rokidkeyboardbridge;
/** One UI attempt. A timed-out waiting callback cannot display later. */
final class NotificationDispatchGate {
 private int state;private boolean acknowledge;
 synchronized boolean begin(){if(state!=0)return false;state=1;return true;}
 synchronized void finish(boolean accepted){if(state!=1)return;acknowledge=accepted;state=2;}
 synchronized void cancelWaiting(){if(state==0)state=3;}
 synchronized boolean acknowledged(){return state==2&&acknowledge;}
}
