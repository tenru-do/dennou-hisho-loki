package com.example.lokihealbe;
import android.content.*;
/** Restore the persisted fallback without launching UI or a foreground service at boot. */
public final class SyncRecoveryReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){String action=intent.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))SyncJob.registerFallback(context);}
}
