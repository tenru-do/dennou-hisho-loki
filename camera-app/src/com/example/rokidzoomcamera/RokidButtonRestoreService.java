package com.example.rokidzoomcamera;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RokidButtonRestoreService extends Service {
    static final String ACTION_START_GUARD = "com.example.rokidzoomcamera.START_BUTTON_GUARD";
    static final String ACTION_RESTORE = "com.example.rokidzoomcamera.RESTORE_BUTTON";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RESTORE.equals(intent.getAction())) {
            RokidOfficialButtonControl.restore(this, "restore service explicit");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        RokidOfficialButtonControl.restore(this, "restore service task removed");
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        RokidOfficialButtonControl.restore(this, "restore service destroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
