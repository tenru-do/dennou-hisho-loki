package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private, independent store for user-requested dynamic memories. Newest entry is first. */
public final class MemoryManager {
    private static final Object STORE_LOCK = new Object();
    private final AtomicFile file;

    public MemoryManager(Context context) {
        file = new AtomicFile(new File(context.getFilesDir(), "memories.json"));
    }

    private JSONArray read() throws Exception {

        byte[] data;
        // openRead restores AtomicFile's backup before deciding the store is absent.
        FileInputStream opened;
        try {
            opened = file.openRead();
        } catch (java.io.FileNotFoundException error) {
            if (!file.getBaseFile().exists()
                    && !new File(file.getBaseFile().getPath() + ".bak").exists()) return new JSONArray();
            throw error;
        }
        try (FileInputStream input = opened) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            data = output.toByteArray();
        }
        return new JSONArray(new String(data, StandardCharsets.UTF_8));
    }

    private void write(JSONArray entries) throws Exception {
        FileOutputStream output = file.startWrite();
        try {
            output.write(entries.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(output);
        } catch (Exception error) {
            file.failWrite(output);
            throw error;
        }
    }

    public JSONArray getAll() throws Exception {
        synchronized (STORE_LOCK) {
        return new JSONArray(read().toString());
        }
    }

    /** Administrative full replacement. Phone UI uses per-entry compare-and-set via exchange. */
    public void replaceAll(JSONArray entries) throws Exception {
        synchronized (STORE_LOCK) {
        if (entries == null) throw new IllegalArgumentException("entries is null");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (entry.optString("id").isEmpty() || entry.optString("timestamp").isEmpty()
                    || entry.optString("topic").isEmpty() || entry.optString("content").isEmpty()) {
                throw new IllegalArgumentException("invalid memory at index " + i);
            }
        }
        write(new JSONArray(entries.toString()));
        }
    }

    public JSONObject save(String topic, String content) throws Exception {
        synchronized (STORE_LOCK) {
        if (topic == null || topic.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("topic and content are required");
        }
        Date now = new Date();
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(now);
        String id = "mem_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(now)
                + "_" + UUID.randomUUID().toString().substring(0, 8);
        JSONObject entry = new JSONObject().put("id", id).put("timestamp", stamp)
                .put("topic", topic.trim()).put("content", content.trim());
        JSONArray existing = read();
        JSONArray updated = new JSONArray().put(entry);
        for (int i = 0; i < existing.length(); i++) updated.put(existing.getJSONObject(i));
        write(updated);
        return entry;
        }
    }

    public JSONObject delete(String target) throws Exception {
        synchronized (STORE_LOCK) {
        JSONArray existing = read();
        if (existing.length() == 0) return null;
        int index = -1;
        if ("latest".equals(target)) index = 0;
        else for (int i = 0; i < existing.length(); i++) {
            JSONObject entry = existing.getJSONObject(i);
            if (entry.optString("id").equals(target)) { index = i; break; }
        }
        if (index < 0) return null;
        JSONObject removed = existing.getJSONObject(index);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < existing.length(); i++) if (i != index) updated.put(existing.getJSONObject(i));
        write(updated);
        return removed;
        }
    }

    /** Compare-and-set one entry; never replace unrelated memories from a phone snapshot. */
    public JSONObject exchange(JSONObject request) throws Exception {
        synchronized (STORE_LOCK) {
            JSONObject operation = request.optJSONObject("operation");
            JSONObject response = new JSONObject().put("requestId", request.optString("requestId"))
                    .put("operationId", operation == null ? "" : operation.getString("id"));
            boolean ok = true; String error = "";
            if (operation != null) {
                try {
                    JSONObject expected = operation.getJSONObject("expected");
                    String id = expected.getString("id");
                    if (id.isEmpty() || "latest".equals(id)) throw new IllegalArgumentException("invalid_id");
                    String action = operation.getString("action");
                    if (!"edit".equals(action) && !"delete".equals(action)) throw new IllegalArgumentException("invalid_action");
                    JSONArray entries = read(); int index = -1;
                    for (int i=0;i<entries.length();i++) if (id.equals(entries.getJSONObject(i).getString("id"))) { index=i; break; }
                    // Delete retries after an acknowledgement is lost are successful no-ops.
                    if (index < 0 && "edit".equals(action)) throw new IllegalArgumentException("メモが既に削除されています");
                    if (index >= 0) {
                        JSONObject current=entries.getJSONObject(index);
                        String topic=operation.optString("topic").trim(), content=operation.optString("content").trim();
                        boolean replay="edit".equals(action) && current.optString("topic").equals(topic)
                                && current.optString("content").equals(content)
                                && current.optString("timestamp").equals(expected.getString("timestamp"));
                        if (!replay && (!current.getString("topic").equals(expected.getString("topic"))
                                || !current.getString("content").equals(expected.getString("content"))
                                || !current.getString("timestamp").equals(expected.getString("timestamp"))))
                            throw new IllegalArgumentException("グラス側のメモが更新されました");
                        if ("edit".equals(action)) {
                            if(topic.isEmpty() || content.isEmpty() || topic.length()>200 || content.length()>12000)
                                throw new IllegalArgumentException("invalid_content");
                            current.put("topic",topic).put("content",content);
                            if (!replay) write(entries);
                        } else {
                            JSONArray updated=new JSONArray();
                            for(int i=0;i<entries.length();i++) if(i!=index) updated.put(entries.getJSONObject(i));
                            write(updated);
                        }
                    }
                } catch (IllegalArgumentException | org.json.JSONException rejected) {
                    ok=false; error=rejected.getMessage();
                }
            }
            return response.put("ok",ok).put("error",error).put("entries",read());
        }
    }
    public String promptContext(int limit) throws Exception {
        synchronized (STORE_LOCK) {
        JSONArray entries = read();
        JSONObject reference = new JSONObject()
                .put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(new Date()));
        JSONArray selected = new JSONArray();
        for (int i = 0; i < Math.min(Math.max(0, limit), entries.length()); i++) {
            selected.put(entries.getJSONObject(i));
        }
        reference.put("memories", selected);
        return reference.toString();
    }
    }
}