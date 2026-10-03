package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/** Durable phone cache/outbox. Only an authenticated glass acknowledgement changes the cache. */
public final class DynamicMemorySync {
    private static final Object LOCK = new Object();
    private final SharedPreferences prefs;
    public DynamicMemorySync(Context context) {
        prefs = context.getSharedPreferences("dynamic_memory_sync", Context.MODE_PRIVATE);
    }
    public JSONObject state() throws Exception {
        synchronized (LOCK) {
            return new JSONObject().put("entries", new JSONArray(prefs.getString("entries", "[]")))
                    .put("receivedAt", prefs.getLong("receivedAt", 0))
                    .put("requestId", prefs.getString("requestId", ""))
                    .put("pending", !prefs.getString("operation", "").isEmpty())
                    .put("message", prefs.getString("message", "グラスから未取得です"));
        }
    }
    public void refresh() throws Exception {
        synchronized (LOCK) {
            commit(prefs.edit().putString("requestId", UUID.randomUUID().toString())
                    .putString("message", "取得待ち。グラスの接続とアプリ起動を確認してください"));
        }
    }
    public void queue(JSONObject expected, String action, String topic, String content) throws Exception {
        synchronized (LOCK) {
            if (!prefs.getString("operation", "").isEmpty()) throw new IllegalStateException("前の変更の応答を待っています");
            if (!"edit".equals(action) && !"delete".equals(action)) throw new IllegalArgumentException("操作が無効です");
            if ("edit".equals(action) && (topic.trim().isEmpty() || content.trim().isEmpty()
                    || topic.length() > 200 || content.length() > 12000)) throw new IllegalArgumentException("件名1〜200文字、本文1〜12000文字で入力してください");
            JSONObject operation = new JSONObject().put("id", UUID.randomUUID().toString())
                    .put("action", action).put("expected", expected)
                    .put("topic", topic.trim()).put("content", content.trim());
            commit(prefs.edit().putString("operation", operation.toString())
                    .putString("message", "変更の同期待ち。グラスの確認応答後に反映します"));
        }
    }
    public JSONObject poll() throws Exception {
        synchronized (LOCK) {
            String operation = prefs.getString("operation", "");
            return new JSONObject().put("requestId", prefs.getString("requestId", ""))
                    .put("operation", operation.isEmpty() ? JSONObject.NULL : new JSONObject(operation));
        }
    }
    public JSONObject accept(JSONObject response) throws Exception {
        synchronized (LOCK) {
            String request = response.optString("requestId");
            String ack = response.optString("operationId");
            String operation = prefs.getString("operation", "");
            boolean matchedOperation = !operation.isEmpty() && ack.equals(new JSONObject(operation).getString("id"));
            boolean matchedRequest = !request.isEmpty() && request.equals(prefs.getString("requestId", ""));
            if (!matchedOperation && !matchedRequest) return new JSONObject().put("ok", true).put("ignored", true);
            JSONArray entries = response.getJSONArray("entries");
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            for (int i=0; i<entries.length(); i++) {
                JSONObject item=entries.getJSONObject(i);
                if (item.getString("id").isEmpty() || !ids.add(item.getString("id"))
                        || item.getString("timestamp").isEmpty() || item.getString("topic").isEmpty()
                        || item.getString("content").isEmpty()) throw new IllegalArgumentException("invalid memory snapshot");
            }
            SharedPreferences.Editor edit = prefs.edit().putString("entries", entries.toString())
                    .putLong("receivedAt", System.currentTimeMillis());
            if (matchedRequest) edit.remove("requestId");
            if (matchedOperation) edit.remove("operation");
            edit.putString("message", matchedOperation ? (response.optBoolean("ok") ? "変更をグラスで保存しました"
                    : "変更できませんでした: " + response.optString("error") + "。再取得した内容を確認してください") : "グラスから取得しました");
            commit(edit);
            return new JSONObject().put("ok", true);
        }
    }
    private static void commit(SharedPreferences.Editor edit) throws Exception {
        if (!edit.commit()) throw new java.io.IOException("記憶同期状態を保存できませんでした");
    }
}