package com.example.rokidkeyboardbridge;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Run on Android with app_process; uses only disposable in-memory preferences and fake network. */
public final class SharedMemoryRegressionTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void drain(SharedMemoryManager manager) throws Exception {
        Field field = SharedMemoryManager.class.getDeclaredField("worker");
        field.setAccessible(true);
        ((ExecutorService) field.get(manager)).submit(() -> {}).get(5, TimeUnit.SECONDS);
    }
    public static void main(String[] args) throws Exception {
        Map<String, Object> store = new HashMap<>();
        Map<String, Object> pending = new HashMap<>();
        AtomicBoolean writable = new AtomicBoolean(true);
        SharedPreferences.Editor editor = (SharedPreferences.Editor) Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(), new Class[]{SharedPreferences.Editor.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("putString") || method.getName().equals("putLong")) {
                        pending.put((String) values[0], values[1]);
                        return proxy;
                    }
                    if (method.getName().equals("remove")) {
                        pending.put((String) values[0], null); return proxy;
                    }
                    if (method.getName().equals("commit")) {
                        if (writable.get()) store.putAll(pending);
                        pending.clear();
                        return writable.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        SharedPreferences prefs = (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class[]{SharedPreferences.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("edit")) return editor;
                    if (method.getName().equals("getString") || method.getName().equals("getLong")) {
                        return store.containsKey(values[0]) ? store.get(values[0]) : values[1];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        ContextWrapper context = new ContextWrapper(null) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) { return prefs; }
        };
        String good = new JSONObject().put("loki_persona", "穏やかなロキ")
                .put("pinned_context", new JSONObject().put("task", "PCの固定チャットを引き継ぐ"))
                .put("user_context", new JSONObject().put("location", "立川市"))
                .put("recent_topics", new JSONArray().put("旅行"))
                .put("shared_memory", new JSONArray().put("コーヒーは無糖")).toString();
        AtomicReference<String> response = new AtomicReference<>(good);
        AtomicBoolean offline = new AtomicBoolean(false);
        SharedMemoryManager.Source source = () -> {
            if (offline.get()) throw new IOException("offline");
            return response.get();
        };
        SharedMemoryManager manager = new SharedMemoryManager(context, source);
        drain(manager);
        check(manager.getContext().isEmpty(), "first start without cache");
        manager.refreshAsync(); drain(manager);
        String first = manager.getContext();
        check(first.contains("立川市") && first.contains("旅行") && first.contains("無糖")
                && first.contains("穏やか") && first.contains("【共有記憶】")
                && first.contains("【ピン留め文脈】") && first.contains("固定チャット"), "all five fields rendered");
        JSONObject legacy = new JSONObject(good);
        legacy.remove("pinned_context");
        check(SharedMemoryManager.buildContext(legacy.toString()).contains("無糖"), "old cache remains usable");
        check(good.equals(store.get(SharedMemoryManager.FILE_ID)), "successful JSON persisted");
        response.set("<html>sign in</html>"); manager.refreshAsync(); drain(manager);
        check(first.equals(manager.getContext()), "HTML preserves success");
        response.set("{}"); manager.refreshAsync(); drain(manager);
        check(first.equals(manager.getContext()), "invalid schema preserves success");
        offline.set(true); manager.refreshAsync(); drain(manager);
        check(first.equals(manager.getContext()), "offline preserves success");
        SharedMemoryManager restarted = new SharedMemoryManager(context, source);
        JSONObject startupRequest = new JSONObject();
        restarted.appendToRequest(startupRequest);
        check(startupRequest.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text").contains("固定チャット"), "first API worker request loads cache");
        drain(restarted);
        check(first.equals(restarted.getContext()), "restart loads cache without network");
        Field fetchedAt = SharedMemoryManager.class.getDeclaredField("contextFetchedAt");
        fetchedAt.setAccessible(true);
        fetchedAt.setLong(restarted, System.currentTimeMillis() - CacheFreshnessPolicy.SHARED_MEMORY_TTL_MS);
        check(restarted.getContext().isEmpty(), "expired RAM cache is not referenced");
        JSONObject expiredRequest = new JSONObject();
        restarted.appendToRequest(expiredRequest);
        check(!expiredRequest.has("systemInstruction"), "expired cache omitted from API");
        store.put(SharedMemoryManager.CACHE_FETCHED_AT, System.currentTimeMillis() - CacheFreshnessPolicy.SHARED_MEMORY_TTL_MS);
        store.put("unrelated_setting", "preserved");
        SharedMemoryManager expired = new SharedMemoryManager(context, source); drain(expired);
        check(expired.getContext().isEmpty(), "expired disk cache is not restored");
        check(store.get(SharedMemoryManager.FILE_ID) == null && "preserved".equals(store.get("unrelated_setting")),
                "expiration removes only shared cache keys");
        store.put(SharedMemoryManager.FILE_ID, good);
        store.remove(SharedMemoryManager.CACHE_FETCHED_AT);
        SharedMemoryManager undated = new SharedMemoryManager(context, source); drain(undated);
        check(undated.getContext().isEmpty(), "legacy undated cache is not restored");
        store.put(SharedMemoryManager.FILE_ID, good);
        store.put(SharedMemoryManager.CACHE_FETCHED_AT, System.currentTimeMillis());
        offline.set(false); response.set(good.replace("旅行", "散歩"));
        writable.set(false); manager.refreshAsync(); drain(manager);
        check(first.equals(manager.getContext()) && good.equals(store.get(SharedMemoryManager.FILE_ID)),
                "failed persistence preserves last durable success");
        writable.set(true); manager.refreshAsync(); drain(manager);
        check(manager.getContext().contains("散歩"), "later refresh updates context");
        JSONObject request = new JSONObject().put("contents", new JSONArray().put("current location/time"))
                .put("tools", new JSONArray().put(new JSONObject().put("google_search", new JSONObject())))
                .put("systemInstruction", new JSONObject().put("parts",
                        new JSONArray().put(new JSONObject().put("text", "local memory rules"))));
        String contents = request.getJSONArray("contents").toString();
        String tools = request.getJSONArray("tools").toString();
        manager.appendToRequest(request);
        check(request.getJSONObject("systemInstruction").getJSONArray("parts").length() == 2,
                "existing system instruction preserved");
        check(contents.equals(request.getJSONArray("contents").toString())
                && tools.equals(request.getJSONArray("tools").toString()), "contents and grounding preserved");
        try { SharedMemoryManager.buildContext(good + "garbage"); throw new AssertionError("trailing data"); }
        catch (org.json.JSONException expected) { }
        response.set("{\"loki_persona\":null,\"user_context\":{},\"recent_topics\":[],\"shared_memory\":[]}");
        manager.refreshAsync(); drain(manager);
        check(manager.getContext().isEmpty(), "valid empty file clears obsolete context");
        System.out.println("PASS: fields, cache, restart, malformed/schema/offline/write failures, refresh, merge, empty data");
        System.exit(0);
    }
}
