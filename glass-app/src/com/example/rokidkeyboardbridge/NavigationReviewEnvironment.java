package com.example.rokidkeyboardbridge;
import android.app.Activity;
/** In-process dependencies for isolated interaction verification. Production uses the Activity and bridge. */
final class NavigationReviewEnvironment {
 interface Transport {String request(String path,boolean mutation)throws Exception;}
 final Activity dialogs;final Transport transport;final Runnable started;final long timeoutMs;
 NavigationReviewEnvironment(Activity dialogs,Transport transport,Runnable started,long timeoutMs){this.dialogs=dialogs;this.transport=transport;this.started=started;this.timeoutMs=timeoutMs;}
}
