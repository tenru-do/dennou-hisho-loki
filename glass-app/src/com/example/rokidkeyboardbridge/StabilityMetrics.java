package com.example.rokidkeyboardbridge;
import android.content.Context;import android.os.SystemClock;import org.json.*;import java.util.concurrent.atomic.AtomicLong;
/** Numeric counters only; no network calls or URLs. Counts attempts, not successes or device-wide traffic. */
public final class StabilityMetrics {
 private static final AtomicLong network=new AtomicLong();private static final String session=java.util.UUID.randomUUID().toString();private static long saved=-1;
 public static void networkAttempt(){network.incrementAndGet();}
 public static synchronized void flush(Context c){long now=SystemClock.elapsedRealtime();if(saved>=0&&now>=saved&&now-saved<60000)return;saved=now;c.getSharedPreferences("loki_stability_metrics",0).edit().putLong("networkAttempts",network.get()).putLong("sampleElapsedMs",now).putString("session",session).apply();}
 public static JSONObject snapshot(){try{return new JSONObject().put("networkAttempts",network.get()).put("session",session).put("scope","instrumented calls in this process; attempts, not successes or device-wide traffic");}catch(Exception ignored){return new JSONObject();}}
 private StabilityMetrics(){}
}
