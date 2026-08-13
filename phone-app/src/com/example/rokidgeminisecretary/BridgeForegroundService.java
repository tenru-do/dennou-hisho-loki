package com.example.rokidgeminisecretary;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public final class BridgeForegroundService extends Service {
    private static final String CHANNEL_ID = "rokid_secretary_bridge";
    private static final int NOTIFICATION_ID = 8765;

    @Override
    public void onCreate() {
        super.onCreate();
        startBridgeForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startBridgeForeground();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBridgeForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Denno Hisho Loki Bridge",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Keeps the Rokid bridge active.");
                manager.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle("Denno Hisho Loki")
                .setContentText("Rokid bridge is active")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setOngoing(true);
        return builder.build();
    }
}
