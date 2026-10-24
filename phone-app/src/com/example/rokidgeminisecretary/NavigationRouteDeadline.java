package com.example.rokidgeminisecretary;

import java.io.InterruptedIOException;
import java.util.function.LongSupplier;
import org.json.JSONException;
import org.json.JSONObject;

/** Bounds the complete search. A timed-out worker retains its slot until it exits. */
final class NavigationRouteDeadline {
    static final long LIMIT_MS = 75000;
    private static final ThreadLocal<NavigationRouteDeadline> current = new ThreadLocal<>();
    private final LongSupplier clock;
    private final long start;

    NavigationRouteDeadline() { this(android.os.SystemClock::elapsedRealtime); }
    NavigationRouteDeadline(LongSupplier clock) { this.clock = clock; start = clock.getAsLong(); }
    boolean expired() {
        long now = clock.getAsLong();
        return now < start || now - start >= LIMIT_MS;
    }
    static void enter(NavigationRouteDeadline lease) { current.set(lease); }
    static void leave() { current.remove(); }
    static void check() throws InterruptedIOException {
        NavigationRouteDeadline deadline = current.get();
        if (deadline != null && deadline.expired()) throw new InterruptedIOException("google_route_timeout");
    }

    static JSONObject present(JSONObject packet, boolean worker, boolean expired) throws JSONException {
        boolean pending = worker && !expired;
        boolean orphan = "fetching".equals(packet.optString("routeStatus")) && !pending;
        if (expired || orphan) {
            packet.put("routeRefreshing", false).put("routeReady", false).put("routeStatus", "request_failed")
                    .put("instruction", expired ? "経路検索が時間切れです" : "経路を取得できませんでした")
                    .put("recoveryMessage", expired && worker
                            ? "前の検索を終了中です。自動で重複検索はしません。ナビの状態で確認してください"
                            : "通信・現在地を確認し、ナビの状態から再取得してください");
            String error = packet.optString("routeError");
            packet.put("routeError", expired ? "google_route_timeout"
                    : error.isEmpty() ? "google_route_result_unavailable" : error);
            packet.remove("journey");
            for (String key : new String[]{"nextDistance", "afterNextInstruction", "afterNextDistance",
                    "afterNextDuration", "totalRemainingDistance", "totalRemainingDuration", "routeArrival", "arrival"}) {
                packet.put(key, "");
            }
        }
        return packet;
    }
}
