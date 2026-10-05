package com.example.rokidzoomcamera;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

final class RokidOfficialButtonControl {
    static final String TAG = "RokidZoomCamera";
    static final String ACTION_ASSIST_COMMAND = "com.rokid.os.master.assist.server.cmd";
    static final String ASSIST_SERVER_PACKAGE = "com.rokid.os.sprite.assistserver";
    static final String ASSIST_MASTER_SERVICE = "com.rokid.os.sprite.assist.MasterAssistService";
    static final String KEY_SHORT_PRESS_FUNCTION = "settings_interaction_shortPressFun";
    static final String VALUE_OFF = "none";
    static final String VALUE_PICTURE = "picture";

    private RokidOfficialButtonControl() {
    }

    static void startAssistService(Context context) {
        try {
            Intent service = new Intent();
            service.setClassName(ASSIST_SERVER_PACKAGE, ASSIST_MASTER_SERVICE);
            service.setAction(ASSIST_MASTER_SERVICE);
            context.startService(service);
        } catch (Exception e) {
            Log.w(TAG, "assist service start failed: " + e.getMessage());
        }
    }

    static void setShortPressFunction(Context context, String value, String reason) {
        startAssistService(context);
        String json = "[{\"key\":\"" + KEY_SHORT_PRESS_FUNCTION + "\",\"value\":\"" + value + "\"}]";
        Intent intent = new Intent(ACTION_ASSIST_COMMAND);
        intent.setPackage(ASSIST_SERVER_PACKAGE);
        intent.putExtra("cmd_type", "setting_change");
        intent.putExtra("value", json);
        context.sendBroadcast(intent);
        Log.d(TAG, reason + " key=" + KEY_SHORT_PRESS_FUNCTION + " value=" + value);
    }

    static void claim(Context context, String reason) {
        setShortPressFunction(context, VALUE_OFF, reason);
    }

    static void restore(Context context, String reason) {
        setShortPressFunction(context, VALUE_PICTURE, reason);
    }
}
