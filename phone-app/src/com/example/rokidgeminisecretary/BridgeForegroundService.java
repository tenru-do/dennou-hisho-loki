package com.example.rokidgeminisecretary;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class BridgeForegroundService extends Service {
    private static final String CHANNEL_ID = "rokid_secretary_bridge";
    private static final int NOTIFICATION_ID = 8765;
    private final Handler morningHandler = new Handler(Looper.getMainLooper());
    private final Runnable morningCheck = new Runnable() {
        @Override public void run() {
            MorningBriefingManager.ensureFreshAsync(
                    BridgeForegroundService.this, false);
            morningHandler.postDelayed(this, 10L * 60L * 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startBridgeForeground();
        TransitLocationTracker.start(this);
        morningHandler.postDelayed(morningCheck, 3000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startBridgeForeground();
        TransitLocationTracker.start(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        morningHandler.removeCallbacks(morningCheck);
        TransitLocationTracker.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBridgeForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            int serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            if (hasLocationPermission()) {
                serviceTypes |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    serviceTypes);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
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
                .setContentText("Rokid bridge / ロキ・トピック更新 / 駅情報")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setOngoing(true);
        return builder.build();
    }
}
