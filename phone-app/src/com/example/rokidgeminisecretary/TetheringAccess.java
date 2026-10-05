package com.example.rokidgeminisecretary;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

/** Settings fallback, with public API-36 toggle only for an actually privileged installation. */
public final class TetheringAccess {
    private TetheringAccess() { }
    public static void open(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 36
                && activity.checkSelfPermission("android.permission.TETHER_PRIVILEGED")
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { privilegedToggle(activity); return; }
            catch (Exception | LinkageError unavailable) { }
        }
        settings(activity);
    }
    // Reflect only documented public API-36 methods to retain the existing API-35 build.
    // Never infer current hotspot state from a previous app tap or use hidden APIs.
    private static void privilegedToggle(Activity activity) throws Exception {
        Object manager=activity.getSystemService("tethering");
        if(manager==null) throw new IllegalStateException("tethering unavailable");
        Class<?> type=Class.forName("android.net.TetheringManager");
        Class<?> eventType=Class.forName("android.net.TetheringManager$TetheringEventCallback");
        android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        java.util.concurrent.Executor executor=task -> handler.post(task);
        java.util.concurrent.atomic.AtomicBoolean completed=new java.util.concurrent.atomic.AtomicBoolean();
        Object[] event=new Object[1];
        Runnable unregister=()-> { try { type.getMethod("unregisterTetheringEventCallback",eventType).invoke(manager,event[0]); } catch(Exception ignored) { } };
        event[0]=java.lang.reflect.Proxy.newProxyInstance(eventType.getClassLoader(),new Class<?>[]{eventType},(proxy,method,args)-> {
            if(method.getDeclaringClass()==Object.class) return objectMethod(proxy,method.getName(),args);
            if(!method.getName().equals("onTetheredInterfacesChanged") || !completed.compareAndSet(false,true)) return null;
            unregister.run();
            try {
                boolean enabled=false;
                Class<?> iface=Class.forName("android.net.TetheringInterface");
                for(Object item:(java.util.Set<?>)args[0]) if(((Integer)iface.getMethod("getType").invoke(item))==0) enabled=true;
                Class<?> requestType=Class.forName("android.net.TetheringManager$TetheringRequest");
                Class<?> builderType=Class.forName("android.net.TetheringManager$TetheringRequest$Builder");
                Object request=builderType.getMethod("build").invoke(builderType.getConstructor(int.class).newInstance(0));
                Class<?> callbackType=Class.forName("android.net.TetheringManager$"+(enabled?"StopTetheringCallback":"StartTetheringCallback"));
                Object callback=java.lang.reflect.Proxy.newProxyInstance(callbackType.getClassLoader(),new Class<?>[]{callbackType},(p,m,a)-> {
                    if(m.getDeclaringClass()==Object.class) return objectMethod(p,m.getName(),a);
                    if(m.getName().endsWith("Failed")) settings(activity);
                    else if(m.getName().endsWith("Started") || m.getName().endsWith("Succeeded"))
                        Toast.makeText(activity,"テザリングを切り替えました",Toast.LENGTH_SHORT).show();
                    return null;
                });
                type.getMethod(enabled?"stopTethering":"startTethering",requestType,java.util.concurrent.Executor.class,callbackType)
                        .invoke(manager,request,executor,callback);
            } catch(Exception | LinkageError unavailable) { settings(activity); }
            return null;
        });
        try { type.getMethod("registerTetheringEventCallback",java.util.concurrent.Executor.class,eventType).invoke(manager,executor,event[0]); }
        catch(Exception failed) { unregister.run(); throw failed; }
        handler.postDelayed(()-> { if(completed.compareAndSet(false,true)) { unregister.run(); settings(activity); } },2000);
    }
    private static Object objectMethod(Object proxy,String method,Object[] args) {
        if("hashCode".equals(method)) return System.identityHashCode(proxy);
        if("equals".equals(method)) return proxy==args[0];
        return "TetheringCallback";
    }
    private static void settings(Activity activity) {
        // OEMs may omit the dedicated activity; launch and catch rather than relying
        // on package visibility or a hidden WifiManager/ConnectivityManager API.
        Intent[] choices = {
            new Intent("android.settings.TETHER_SETTINGS"),
            new Intent().setClassName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity"),
            new Intent(Settings.ACTION_WIRELESS_SETTINGS), new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent intent : choices) {
            try { activity.startActivity(intent); return; }
            catch (android.content.ActivityNotFoundException | SecurityException unavailable) { }
        }
        Toast.makeText(activity,"テザリング設定を開けません。端末の設定から操作してください",Toast.LENGTH_LONG).show();
    }
}