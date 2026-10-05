package com.example.rokidzoomcamera;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class RokidButtonAccessibilityService extends AccessibilityService {
    private static final String TAG = "RokidButtonA11y";
    static final String ACTION_HARDWARE_CAPTURE = "com.example.rokidzoomcamera.ACTION_HARDWARE_CAPTURE";
    static final String PREFS = "rokid_zoom_camera";
    static final String PREF_APP_ACTIVE = "app_active";

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Log.d(TAG, "key " + KeyEvent.keyCodeToString(event.getKeyCode())
                + " action=" + event.getAction()
                + " active=" + isMainAppActive());
        if (event.getAction() != KeyEvent.ACTION_UP || !isMainAppActive()) {
            return false;
        }

        int keyCode = event.getKeyCode();
        String keyName = KeyEvent.keyCodeToString(keyCode);
        if (keyCode == KeyEvent.KEYCODE_CAMERA
                || keyCode == KeyEvent.KEYCODE_STEM_PRIMARY) {
            Intent intent = new Intent(ACTION_HARDWARE_CAPTURE);
            intent.setPackage(getPackageName());
            intent.putExtra("keyCode", keyCode);
            intent.putExtra("keyName", keyName);
            sendBroadcast(intent);
            Log.d(TAG, "consumed " + keyName);
            return true;
        }

        return false;
    }

    private boolean isMainAppActive() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getBoolean(PREF_APP_ACTIVE, false);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "connected");
    }

    @Override
    public void onInterrupt() {
    }
}
