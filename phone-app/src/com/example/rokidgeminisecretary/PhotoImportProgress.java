package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONObject;

/** A read-only browser view of verified transfer receipts, separate from the photo-list size. */
final class PhotoImportProgress {
    static JSONObject latestPhoto(JSONObject workspace) {
        JSONArray rows = workspace.optJSONArray("transfers");
        JSONObject latest = null;
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && "photo".equals(row.optString("kind"))
                    && (latest == null || row.optLong("startedAt") > latest.optLong("startedAt"))) latest = row;
        }
        return latest;
    }

    static JSONObject view(JSONObject workspace, JSONObject request, long now) throws Exception {
        JSONObject out = new JSONObject().put("state", "IDLE").put("active", false).put("fresh", false)
                .put("totalKnown", false).put("total", JSONObject.NULL).put("confirmed", JSONObject.NULL)
                .put("remaining", JSONObject.NULL).put("retained", 0).put("failed", 0).put("deferred", 0)
                .put("label", "KEEP写真の取り込み状況は、コピーの依頼後に表示します。");
        long requestedAt = request.optLong("createdAt");
        boolean requested = request.optString("id").matches("[a-f0-9-]{36}") && requestedAt > 0;
        JSONObject row = latestPhoto(workspace);
        // A new request must never inherit the previous batch's completion or counts.
        if (row != null && requested && (row.optLong("startedAt") < requestedAt
                || row.optString("batchId").equals(request.optString("previousBatchId")))) row = null;
        if (row == null) {
            if (!requested) return out;
            if (now < requestedAt) return out.put("state", "UNKNOWN")
                    .put("label", "端末の時刻が変わったため、取り込み状況を確認できません。保存完了は未確認です。接続と原本を確認してください。");
            if (now > request.optLong("expiresAt")) return out.put("state", "EXPIRED")
                    .put("label", "取り込み要求の期限が切れました。保存完了は未確認です。接続と原本を確認してください。");
            long acceptedAt = request.optLong("acceptedAt");
            boolean accepted = acceptedAt > 0 && acceptedAt <= now;
            String phase = accepted ? now - acceptedAt > 30000 ? "START_UNCONFIRMED" : "ACCEPTED" : "QUEUED";
            String text = accepted ? now - acceptedAt > 30000
                    ? "グラスは受信済みですが、取り込み開始は未確認です。対象枚数・残り枚数はまだ取得できていません。"
                    : "グラスが受信しました。KEEPの停止・対象枚数の確認待ちです。まだ保存完了ではありません。"
                    : "グラスへ取り込みを依頼しました。受信・対象枚数の確認待ちです。まだ保存完了ではありません。";
            return out.put("state", phase).put("active", true).put("label", text);
        }
        JSONObject safe = TransferProgressPolicy.safe(row);
        long total = safe.getLong("expected"), confirmed = safe.getLong("confirmed"), retained = safe.getLong("retained");
        long remaining = Math.max(0, total - confirmed - retained), updatedAt = safe.optLong("updatedAt");
        String phase = safe.getString("phase");
        boolean fresh = workspace.optBoolean("connected") && updatedAt > 0 && now >= updatedAt && now - updatedAt <= 30000;
        String status = phase.equals("RUNNING") ? "取り込み中" : phase.equals("COMPLETED") ? "取り込み処理完了"
                : phase.equals("PARTIAL") ? "一部未完了" : phase.equals("FAILED") ? "取り込み失敗"
                : phase.equals("INTERRUPTED") ? "取り込み中断" : "結果未確認";
        String label = (fresh ? "" : "前回確認した状態（現在の進行は未確認）：") + status
                + " ／ スマホ保存確認 " + confirmed + "枚 ／ 対象 " + total + "枚 ／ 残り未確認 " + remaining + "枚";
        if (retained > 0) label += " ／ 削除状態の照合 " + retained + "枚（コピー枚数には含めません）";
        if (safe.optLong("failed") > 0) label += " ／ 失敗 " + safe.getLong("failed") + "枚";
        if (safe.optLong("deferred") > 0) label += " ／ 別処理待ち " + safe.getLong("deferred") + "枚";
        if (remaining > 0) label += "（残り未確認には失敗・別処理待ちを含みます）";
        return out.put("state", phase).put("active", phase.equals("RUNNING")).put("fresh", fresh)
                .put("batchId", safe.getString("batchId")).put("updatedAt", updatedAt).put("totalKnown", true)
                .put("total", total).put("confirmed", confirmed).put("remaining", remaining)
                .put("retained", retained).put("failed", safe.getLong("failed")).put("deferred", safe.getLong("deferred"))
                .put("label", label);
    }

    private PhotoImportProgress() { }
}
