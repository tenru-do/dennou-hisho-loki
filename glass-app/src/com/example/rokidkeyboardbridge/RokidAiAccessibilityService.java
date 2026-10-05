package com.example.rokidkeyboardbridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** Passively observes text exposed by the RokidAI UI for expression selection. */
public final class RokidAiAccessibilityService extends AccessibilityService {
    private static final String TAG = "RokidAiDirect";
    public static final String ACTION_ROKID_AI_TEXT =
            "com.example.rokidkeyboardbridge.ROKID_AI_TEXT";
    public static final String EXTRA_TEXT = "text";
    private static final int MAX_TEXT_CHARS = 1600;
    private static final long DUPLICATE_WINDOW_MS = 1200L;
    private String lastText = "";
    private long lastTextAt;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();
        if (!"com.rokid.os.sprite.launcher".equals(packageName)
                && !"com.rokid.os.sprite.assistserver".equals(packageName)
                && !"com.rokid.os.sprite.live".equals(packageName)) return;
        StringBuilder text = new StringBuilder();
        for (CharSequence value : event.getText()) append(text, value);
        append(text, event.getContentDescription());
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) appendNodeText(text, root);
        Log.i(TAG, "event package=" + packageName + " type=" + event.getEventType()
                + " textChars=" + text.length() + " root=" + (root != null));
        if (root != null) root.recycle();
        String value = text.toString().trim();
        if (value.length() < 2) return;
        if (value.length() > MAX_TEXT_CHARS) value = value.substring(0, MAX_TEXT_CHARS);
        long now = System.currentTimeMillis();
        if (value.equals(lastText) && now - lastTextAt < DUPLICATE_WINDOW_MS) return;
        lastText = value;
        lastTextAt = now;
        Intent intent = new Intent(ACTION_ROKID_AI_TEXT);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_TEXT, value);
        sendBroadcast(intent);
    }

    private static void append(StringBuilder out, CharSequence value) {
        if (value == null) return;
        String text = value.toString().trim();
        if (text.length() == 0) return;
        if (out.length() > 0) out.append('\n');
        out.append(text);
    }

    private static void appendNodeText(StringBuilder out, AccessibilityNodeInfo node) {
        if (node == null || out.length() >= MAX_TEXT_CHARS) return;
        append(out, node.getText());
        append(out, node.getContentDescription());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                appendNodeText(out, child);
                child.recycle();
            }
            if (out.length() >= MAX_TEXT_CHARS) return;
        }
    }

    @Override
    public void onInterrupt() { }
}
