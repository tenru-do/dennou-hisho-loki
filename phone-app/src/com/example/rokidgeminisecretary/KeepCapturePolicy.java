package com.example.rokidgeminisecretary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

/** Technical KEEP capture state only; no image, location, or conversation content. */
final class KeepCapturePolicy {
    static final long PHOTO_COUNT_FRESH_MS = 30000;

    static JSONObject safe(JSONObject in) throws Exception {
        String phase = in.optString("phase"), reason = in.optString("reason");
        if (!phase.matches("STARTING|RUNNING|STOPPED|FAILED|INTERRUPTED")) phase = "UNKNOWN";
        if (!reason.matches("LOW_SPACE|SAVE_ERROR|CAPTURE_ERROR|USER_STOP|INTERRUPTED|")) reason = "";
        JSONObject result = new JSONObject().put("phase", phase).put("reason", reason)
                .put("at", Math.max(0, in.optLong("at")))
                .put("startedAt", Math.max(0, in.optLong("startedAt")))
                .put("saved", Math.max(0, in.optInt("saved")))
                .put("lastSavedAt", Math.max(0, in.optLong("lastSavedAt")))
                .put("availableBytes", Math.max(0, in.optLong("availableBytes")));
        long retained = optionalWholeNumber(in.opt("retainedPhotos"));
        long measuredAt = optionalWholeNumber(in.opt("retainedPhotosAt"));
        if (retained >= 0 && retained <= Integer.MAX_VALUE && measuredAt > 0) {
            result.put("retainedPhotos", (int) retained).put("retainedPhotosAt", measuredAt);
        }
        return result;
    }

    private static long optionalWholeNumber(Object value) {
        return value instanceof Integer || value instanceof Long ? ((Number) value).longValue() : -1;
    }

    static String overview(JSONObject in, boolean connected, long now) throws Exception {
        JSONObject keep = safe(in == null ? new JSONObject() : in);
        int retained = keep.optInt("retainedPhotos", -1);
        long measuredAt = keep.optLong("retainedPhotosAt", 0);
        String photos = "グラスの現在の保存写真：未確認";
        if (retained >= 0 && measuredAt > 0 && now >= measuredAt) {
            String measurement = retained + "枚（" + formatTime(measuredAt) + "確認）";
            photos = connected && now - measuredAt <= PHOTO_COUNT_FRESH_MS
                    ? "グラスの現在の保存写真 " + measurement
                    : photos + " ／ 前回確認 " + measurement;
        }
        return in == null ? photos : photos + "\n" + captureLabel(keep);
    }

    private static String captureLabel(JSONObject keep) throws Exception {
        String phase = keep.getString("phase");
        String status;
        switch (phase) {
            case "FAILED": status = "保存・撮影で停止"; break;
            case "INTERRUPTED": status = "処理中断（再起動など）"; break;
            case "STOPPED": status = "停止"; break;
            case "RUNNING": status = "保存記録あり"; break;
            case "STARTING": status = "開始要求のみ（保存未確認）"; break;
            default: status = "保存状況は未取得";
        }
        String text = "KEEPの撮影記録：" + status;
        if (!phase.equals("UNKNOWN")) text += " ／ この回の撮影累計（保存成功） " + keep.getInt("saved") + "枚";
        String reason = keep.getString("reason");
        if (reason.equals("LOW_SPACE")) text += " ／ 空き容量不足（1GB以上の空きが必要）";
        else if (reason.equals("SAVE_ERROR")) text += " ／ 写真の保存に失敗";
        else if (reason.equals("CAPTURE_ERROR")) text += " ／ カメラ撮影に失敗";
        if (keep.getLong("at") > 0) text += " ／ 記録時刻 " + formatTime(keep.getLong("at"));
        return text;
    }

    private static String formatTime(long timestamp) {
        return new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPAN).format(new Date(timestamp));
    }

    private KeepCapturePolicy() {}
}
