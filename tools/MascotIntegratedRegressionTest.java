package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/** Shell-only offscreen test; reads the test APK's assets, never installs or launches the app. */
public final class MascotIntegratedRegressionTest {
    private static int checks;
    private static void check(boolean result, String label) {
        checks++;
        if (!result) throw new AssertionError(label);
    }
    private static Field field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Object read(Object object, String name) throws Exception { return field(object, name).get(object); }
    private static void write(Object object, String name, Object value) throws Exception { field(object, name).set(object, value); }
    private static Method method(Object object, String name, Class<?>... types) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        try { run(args); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void run(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context system = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        AssetManager assets = AssetManager.class.getConstructor().newInstance();
        check((int) AssetManager.class.getMethod("addAssetPath", String.class).invoke(assets, args[0]) != 0,
                "test APK asset path");
        Resources resources = new Resources(assets, system.getResources().getDisplayMetrics(),
                system.getResources().getConfiguration());
        Context context = new ContextWrapper(system) {
            @Override public AssetManager getAssets() { return assets; }
            @Override public Resources getResources() { return resources; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return (SharedPreferences) Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
                        new Class[]{SharedPreferences.class}, (proxy, called, values) -> {
                            if (called.getName().equals("getString")) return values[1];
                            throw new UnsupportedOperationException(called.getName());
                        });
            }
        };
        MainActivity owner = new MainActivity();
        Method attach = ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
        attach.setAccessible(true);
        attach.invoke(owner, context);
        Class<?> mascotClass = Class.forName("com.example.rokidkeyboardbridge.MainActivity$MascotView");
        Constructor<?> constructor = mascotClass.getDeclaredConstructor(MainActivity.class, Context.class);
        constructor.setAccessible(true);
        View mascot = (View) constructor.newInstance(owner, context);
        check(mascot.getLayerType() == View.LAYER_TYPE_HARDWARE, "hardware layer retained");
        check(((Bitmap[]) read(mascot, "denseFaces")).length == 15, "all dense frames loaded");
        check((boolean) read(mascot, "secondaryLayersReady"), "all hair layers loaded");
        mascot.layout(0, 0, 320, 320);
        Method prepare = method(mascot, "prepareFrame");
        Method draw = method(mascot, "onDraw", Canvas.class);
        Class<? extends Enum> angle = (Class<? extends Enum>) Class.forName(
                "com.example.rokidkeyboardbridge.MainActivity$AngleState");
        Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        for (int cell = 0; cell < 15; cell++) {
            write(mascot, "angleState", Enum.valueOf(angle, "TRANSITION_TO_FRONT"));
            write(mascot, "anglePosition", cell / 14f);
            write(mascot, "turnProgress", .15f + .7f * cell / 14f);
            write(mascot, "turningToFront", true);
            prepare.invoke(mascot);
            check((int) read(mascot, "preparedDenseFrame") == cell, "frame selected " + cell);
            check((boolean) read(mascot, "preparedDensePortrait"), "dense face is visible " + cell);
            String roi = read(mascot, "bitmapDst").toString();
            draw.invoke(mascot, canvas);
            draw.invoke(mascot, canvas);
            check(roi.equals(read(mascot, "bitmapDst").toString()), "draw leaves prepared sizing unchanged");
            check((float) read(mascot, "anglePosition") == cell / 14f, "draw does not advance time");
        }
        write(mascot, "turnProgress", .10f);
        write(mascot, "anglePosition", 0f);
        write(mascot, "idleBlinking", true);
        prepare.invoke(mascot);
        check(!(boolean) read(mascot, "preparedTurnBlink"), "idle blink cannot conceal leading gaze");
        check((float) read(mascot, "preparedGazeOffset") < 0f, "pupils lead front turn");
        write(mascot, "turnProgress", .5f); prepare.invoke(mascot);
        check((boolean) read(mascot, "preparedTurnBlink"), "mid-turn eyes forced closed");
        Object original = read(mascot, "mascotSheet");
        write(mascot, "mascotSheet", null); prepare.invoke(mascot); draw.invoke(mascot, canvas);
        check(!(boolean) read(mascot, "portraitReady"), "cached vector fallback draws without source sheet");
        write(mascot, "mascotSheet", original);
        write(mascot, "attached", true); write(mascot, "renderingActive", true);
        write(mascot, "secondaryFrameScheduled", true);
        write(mascot, "anglePosition", .45f);
        method(mascot, "reconcileRendering").invoke(mascot);
        check(!(boolean) read(mascot, "renderingActive"), "paused host stops attached view");
        check(!(boolean) read(mascot, "secondaryFrameScheduled"), "pause cancels frame callback");
        check((float) read(mascot, "anglePosition") == 0f
                && !(boolean) read(mascot, "preparedTurnBlink"), "pause settles yaw and eyelids");

        SharedMemoryManager memory = new SharedMemoryManager(context, () -> { throw new java.io.IOException(); });
        String data = "{\"loki_persona\":\"秘書官\",\"pinned_context\":\"固定チャット\","
                + "\"recent_topics\":[],\"user_context\":\"東京都立川市\",\"shared_memory\":[]}";
        write(memory, "contextText", SharedMemoryManager.buildContext(data));
        write(owner, "sharedMemoryManager", memory);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        JSONObject base = new JSONObject().put("contents", new JSONArray().put("live location"))
                .put("tools", new JSONArray().put(new JSONObject().put("google_search", new JSONObject())))
                .put("systemInstruction", new JSONObject().put("parts",
                        new JSONArray().put(new JSONObject().put("text", "existing memory rules"))));
        JSONObject request = new JSONObject(base.toString());
        method(owner, "appendGeminiRuntimeContext", JSONObject.class).invoke(owner, request);
        JSONArray parts = request.getJSONObject("systemInstruction").getJSONArray("parts");
        SimpleDateFormat jstDate = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        jstDate.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        check(parts.length() == 3 && parts.getJSONObject(1).getString("text").contains("JST")
                && parts.getJSONObject(1).getString("text").contains(jstDate.format(new Date())), "JST overrides device timezone");
        check(parts.getJSONObject(2).getString("text").contains("固定チャット"), "pinned context enters system instruction");
        check(base.getJSONObject("systemInstruction").getJSONArray("parts").length() == 1,
                "request base stays reusable for tool turns");
        check(base.getJSONArray("contents").toString().equals(request.getJSONArray("contents").toString())
                && base.getJSONArray("tools").toString().equals(request.getJSONArray("tools").toString()),
                "live location and grounding survive pipeline");
        bitmap.recycle();
        assets.close();
        System.out.println("PASS MascotIntegratedRegressionTest: " + checks + " checks");
        System.exit(0);
    }
}
