package com.example.rokidmanagerlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this);
        text.setText("Opening RokidManager...");
        text.setTextSize(18);
        setContentView(text);
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.example.advancedsettingsmanager",
                    "com.example.advancedsettingsmanager.MainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception error) {
            text.setText("RokidManagerを開けませんでした。\n" + error.getMessage());
        }
    }
}
