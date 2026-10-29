package com.example.rokidkeyboardbridge;
/** Short notification wake ownership; never borrows a user's later interaction. */
final class NotificationWakePolicy {
 static final int KEEP=0,BACKGROUND=1,SLEEP=2;
 static int restoreMode(boolean foreground,boolean interactive){return !interactive?SLEEP:!foreground?BACKGROUND:KEEP;}
 static boolean ready(boolean foreground,boolean interactive){return foreground&&interactive;}
 static boolean restore(int mode,boolean latest,boolean unchanged,boolean busy){return (mode==BACKGROUND||mode==SLEEP)&&latest&&unchanged&&!busy;}
 private NotificationWakePolicy(){}
}
