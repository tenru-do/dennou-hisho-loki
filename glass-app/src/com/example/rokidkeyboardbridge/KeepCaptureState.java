package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONObject;

final class KeepCaptureState {
    private static final long COUNT_REFRESH_MS = 15_000L;
    private static long countReadElapsed = -1L;
    private static long countMeasuredAt;
    private static int retainedPhotos = -1;

    static void record(Context context, String phase, String reason, long startedAt, int saved,
                       long lastSavedAt, long available) {
        try {
            JSONObject state = KeepCapturePolicy.safe(new JSONObject().put("phase", phase)
                    .put("reason", reason).put("at", System.currentTimeMillis()).put("startedAt", startedAt)
                    .put("saved", saved).put("lastSavedAt", lastSavedAt).put("availableBytes", available));
            if (!context.getSharedPreferences("keep_capture_state", 0).edit()
                    .putString("last", state.toString()).commit()) {
                Log.w("LokiKeepState", "state commit failed");
            }
        } catch (Exception error) {
            Log.w("LokiKeepState", "state unavailable");
        }
    }

    // Both callers run on a worker; never enumerate the archive on the HUD thread.
    static JSONObject snapshot(Context context, boolean refreshCount) {
        JSONObject state = captureSnapshot(context);
        addRetainedPhotos(context, state, refreshCount);
        return state;
    }

    private static JSONObject captureSnapshot(Context context) {
        try {
            PhotoRecorderService recorder = PhotoRecorderService.active;
            if (recorder != null) return recorder.captureState();
            JSONObject state = KeepCapturePolicy.safe(new JSONObject(
                    context.getSharedPreferences("keep_capture_state", 0).getString("last", "{}")));
            if (state.optString("phase").matches("RUNNING|STARTING")) {
                state.put("phase", "INTERRUPTED").put("reason", "INTERRUPTED");
            }
            return state;
        } catch (Exception error) {
            return new JSONObject();
        }
    }

    private static synchronized void addRetainedPhotos(Context context, JSONObject state, boolean refresh) {
        long now = SystemClock.elapsedRealtime();
        if (refresh || countReadElapsed < 0 || now - countReadElapsed >= COUNT_REFRESH_MS) {
            try {
                retainedPhotos = PhotoArchiveStore.retainedCount(context);
                countMeasuredAt = System.currentTimeMillis();
            } catch (Exception unavailable) {
                retainedPhotos = -1;
            }
            countReadElapsed = SystemClock.elapsedRealtime();
        }
        if (retainedPhotos < 0) return;
        try {
            state.put("retainedPhotos", retainedPhotos).put("retainedPhotosAt", countMeasuredAt);
        } catch (org.json.JSONException ignored) {
            // Both values are integers; leave the count absent if building the snapshot fails.
            state.remove("retainedPhotos");
            state.remove("retainedPhotosAt");
        }
    }

    static synchronized void refreshCountAfterTransfer() {
        countReadElapsed = -1L;
    }

    private KeepCaptureState() { }
}
