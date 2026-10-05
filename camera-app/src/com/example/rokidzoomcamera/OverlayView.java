package com.example.rokidzoomcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverlayView extends View {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayDeque<String> eventLines = new ArrayDeque<>();
    private String mode = "NORMAL";
    private float zoom = 1.0f;
    private String message = "Rokid Zoom Camera";

    public OverlayView(Context context) {
        super(context);
        bgPaint.setColor(0x44000000);
        textPaint.setColor(0xffd8d8d8);
        textPaint.setTextSize(24f);
        textPaint.setShadowLayer(4f, 0f, 2f, 0xff000000);
    }

    public void setStatus(String mode, float zoom) {
        this.mode = mode;
        this.zoom = zoom;
        invalidate();
    }

    public void setMessage(String message) {
        this.message = message;
        invalidate();
    }

    public void log(String line) {
        android.util.Log.i("RokidZoomCamera", line);
        eventLines.addFirst(line);
        while (eventLines.size() > 6) {
            eventLines.removeLast();
        }
        invalidate();
    }

    public List<String> snapshotLines() {
        return new ArrayList<>(eventLines);
    }

    public String getModeText() {
        return mode;
    }

    public float getZoomTextValue() {
        return zoom;
    }

    public String getMessageText() {
        return message;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOverlay(canvas, getWidth(), getHeight(), true);
    }

    public void drawOverlay(Canvas canvas, int width, int height, boolean includeEventLog) {
        textPaint.setTextSize(Math.max(20f, width / 50f));
        float pad = Math.max(18f, width / 60f);
        float line = textPaint.getTextSize() * 1.35f;

        RectF topBox = new RectF(pad, pad, width - pad, pad + line * 2.5f);
        canvas.drawRoundRect(topBox, 10f, 10f, bgPaint);
        canvas.drawText("Mode: " + mode + "    Zoom: "
                + String.format(Locale.US, "%.1fx", zoom), pad * 1.7f, pad + line, textPaint);
        canvas.drawText(message, pad * 1.7f, pad + line * 2f, textPaint);

        if (!includeEventLog) {
            return;
        }

        float bottomHeight = line * 7f;
        RectF bottomBox = new RectF(pad, height - bottomHeight - pad, width - pad, height - pad);
        canvas.drawRoundRect(bottomBox, 10f, 10f, bgPaint);
        float y = height - bottomHeight + line * 0.8f;
        canvas.drawText("Input log", pad * 1.7f, y, textPaint);
        y += line;
        for (String eventLine : eventLines) {
            canvas.drawText(eventLine, pad * 1.7f, y, textPaint);
            y += line;
        }
    }
}
