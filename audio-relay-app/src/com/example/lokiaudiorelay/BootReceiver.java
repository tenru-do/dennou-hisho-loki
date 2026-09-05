package com.example.lokiaudiorelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    static final String EXTRA_BOOT_AUTO_LAUNCH = "boot_auto_launch";
    @Override public void onReceive(Context context, Intent intent) {
        if (!context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.KEY_AUTO_CAPTURE, true)) {
            return;
        }
        Intent launch = new Intent(context, MainActivity.class);
        launch.putExtra(EXTRA_BOOT_AUTO_LAUNCH, true);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(launch);
    }
}
