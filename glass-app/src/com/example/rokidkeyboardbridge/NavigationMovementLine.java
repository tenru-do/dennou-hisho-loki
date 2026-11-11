package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** One small status row. Turn/boarding instructions remain in the ordinary guidance view. */
final class NavigationMovementLine extends TextView {
    static final float TEXT_SIZE_DP = 9f;

    NavigationMovementLine(Context context) {
        super(context);
        // Match the existing physical navigation header size; do not shrink long labels below it.
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP);
        if (Build.VERSION.SDK_INT >= 26) setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE);
        setTextColor(Color.rgb(150, 255, 175));
        setBackgroundColor(Color.TRANSPARENT);
        setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        int padding = Math.round(2 * getResources().getDisplayMetrics().density);
        setPadding(padding, 0, padding, 0);
        setMinimumWidth(0);
        setSingleLine(true);
        setMaxLines(1);
        setHorizontallyScrolling(false);
        setEllipsize(TextUtils.TruncateAt.END);
        setIncludeFontPadding(true);
        setLineSpacing(0f, 1f);
        setFocusable(false);
        setClickable(false);
        setVisibility(View.GONE);
    }

    static boolean isMovement(String text) {
        String value = text == null ? "" : text.trim();
        return value.startsWith("現在の区間：") || value.startsWith("現在の移動：");
    }

    /** Returns true only when this row consumes the current-movement status, never a turn cue. */
    boolean bindAction(String action) {
        boolean consumed = isMovement(action);
        String value = consumed ? action.replaceAll("[\\r\\n\\t]+", " ").trim() : "";
        if (!getText().toString().equals(value)) setText(value);
        setContentDescription(value);
        int visibility = consumed ? View.VISIBLE : View.GONE;
        if (getVisibility() != visibility) setVisibility(visibility);
        return consumed;
    }

    int remainingLines(int originalBudget) {
        return Math.max(1, originalBudget - (getVisibility() == View.VISIBLE ? 1 : 0));
    }
}
