package com.example.rokidkeyboardbridge;

import android.content.ContextWrapper;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Run with app_process using a disposable directory, never the app's private store. */
public final class MemoryRegressionTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        final File directory = new File(args[0]);
        check(directory.mkdirs(), "fresh isolated test directory");
        ContextWrapper context = new ContextWrapper(null) {
            @Override public File getFilesDir() { return directory; }
        };
        MemoryManager memory = new MemoryManager(context);
        check(memory.getAll().length() == 0, "empty store");
        JSONObject first = memory.save("好み", "コーヒーは無糖");
        check(new MemoryManager(context).getAll().getJSONObject(0).getString("content")
                .equals("コーヒーは無糖"), "persistent reload");
        check(new JSONObject(memory.promptContext(20)).getJSONArray("memories")
                .getJSONObject(0).getString("id").equals(first.getString("id")), "context contains deletion ID");
        memory.save("予定", "明日は散歩");
        check(memory.delete("latest").getString("topic").equals("予定"), "delete latest");
        check(memory.delete("missing") == null && memory.getAll().length() == 1, "unknown ID preserves store");
        check(memory.delete(first.getString("id")) != null, "delete by ID");
        memory.replaceAll(new JSONArray().put(first));
        check(memory.getAll().length() == 1, "full replacement");
        try { memory.save("", "invalid"); throw new AssertionError("invalid save accepted"); }
        catch (IllegalArgumentException expected) { }
        File base = new File(directory, "memories.json");
        File backup = new File(directory, "memories.json.bak");
        check(base.renameTo(backup), "simulate interrupted AtomicFile legacy write");
        check(memory.getAll().length() == 1 && base.exists(), "recover backup without base");
        try (FileOutputStream output = new FileOutputStream(base)) {
            output.write("broken-json".getBytes(StandardCharsets.UTF_8));
        }
        try { memory.save("new", "must not overwrite corruption"); throw new AssertionError("corruption ignored"); }
        catch (org.json.JSONException expected) { }
        check(base.length() == "broken-json".length(), "corrupt file preserved");
        check("save_memory".equals(MemoryRequestPolicy.requiredFunction("営業時間は10時、覚えておいて")), "save beats search");
        check("delete_memory".equals(MemoryRequestPolicy.requiredFunction("直近のメモを消して")), "explicit delete");
        for (String request : new String[]{"覚えている？", "覚えておいてという言葉の意味は？", "これは記憶しないで", "写真を保存して"}) {
            check(MemoryRequestPolicy.requiredFunction(request).isEmpty(), "no forced mutation: " + request);
        }
        check(MemoryRequestPolicy.concernsMemory("最新の記憶メモは？"), "recall beats search");
        check(base.delete() && directory.delete(), "cleanup isolated test files");
        System.out.println("PASS: persistence, reload, context IDs, delete, replacement, corruption, backup recovery, request policy");
    }
}
