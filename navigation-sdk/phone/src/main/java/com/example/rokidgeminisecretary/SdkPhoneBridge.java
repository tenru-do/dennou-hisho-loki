package com.example.rokidgeminisecretary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import com.example.lokinavigation.*;
import com.google.android.libraries.navigation.*;
import com.google.android.libraries.mapsplatform.turnbyturn.model.*;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Optional phone-only integration. No key or SDK data is written to logs/disk. */
public final class SdkPhoneBridge {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Navigator navigator;
    private static NavigationSession<Waypoint> session;
    private static boolean keyInitialized, owned, preparing;
    private static int generation;
    private static String destination = "", sessionId = "";
    private static String status = "停止中";
    private static JSONArray route = new JSONArray();
    private static volatile String published;
    private static volatile long publishedAt;
    private static String lastDiagnostic = "";
    private static long completedUntil;
    private static Navigator.ArrivalListener arrivalListener;

    public static JSONObject snapshot() throws Exception {
        String value = published;
        if (value == null) return null;
        if (SystemClock.elapsedRealtime() - publishedAt > 5000) {
            return new JSONObject().put("source", "navigation_sdk").put("navigationActive", false)
                    .put("ok", true).put("sdkSessionId", sessionId).put("time", System.currentTimeMillis());
        }
        JSONObject payload = new JSONObject(value);
        long ttl = Math.max(0, payload.optLong("sdkLaneTtlMs", 0)
                - (SystemClock.elapsedRealtime() - publishedAt));
        payload.put("sdkLaneTtlMs", ttl);
        if (ttl == 0) payload.put("sdkLaneText", "");
        android.util.Log.d("LokiLaneTx", "tx seq=" + payload.optLong("sdkLaneSequence", 0)
                + " active=" + payload.optBoolean("navigationActive")
                + " chars=" + payload.optString("sdkLaneText").length() + " ttl=" + ttl);
        return payload;
    }

    public static void show(final Activity activity, String initial) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input = new EditText(activity);
        input.setSingleLine(true); input.setHint("目的地・住所"); input.setText(initial);
        box.addView(input);
        final CheckBox tolls = new CheckBox(activity);
        tolls.setText("有料道路を避ける"); tolls.setChecked(true); box.addView(tolls);
        final TextView diagnostics = new TextView(activity);
        box.addView(diagnostics);
        final AlertDialog controls = new AlertDialog.Builder(activity).setTitle("車ナビ / " + status).setView(box)
                .setMessage("Googleマップとは別の車ルートを作成します。候補の住所を確認してください。\n1日10・月300目的地まで。料金や道路状況は現地の標識を優先してください。")
                .setPositiveButton("施設・住所を検索", (dialog, which) -> {
                    if (!BuildConfig.NAVIGATION_SDK_ENABLED) { message(activity, "Cloud設定確認待ちのため、まだ有効化されていません"); return; }
                    search(activity, input.getText().toString().trim(), tolls.isChecked());
                })
                .setNeutralButton("SDKナビ停止", (dialog, which) -> { stop(); message(activity, status); })
                .setNegativeButton("閉じる", null).create();
        final Runnable refresh = new Runnable() {
            @Override public void run() {
                if (!controls.isShowing()) return;
                diagnostics.setText("状態: " + status + "\n" + laneStatus() + "\n" + laneText()
                        + "\n経路点数: " + route.length()
                        + "\nGoogle Mapsの施設検索：地域名＋店名などで検索。検索は1日10回まで。");
                MAIN.postDelayed(this, 1000);
            }
        };
        controls.setOnDismissListener(dialog -> MAIN.removeCallbacks(refresh));
        controls.show(); refresh.run();
    }

    private static String laneStatus() {
        if (!owned || session == null || session.state() == NavigationSession.State.STOPPED)
            return "車線: ナビ停止中";
        if (session.state() == NavigationSession.State.FAILED) return "車線: 経路取得失敗";
        NavInfo info = LaneFeedService.navInfo();
        if (info == null) return "車線: 案内データ受信待ち（未受信または5秒超過）";
        if (info.getNavState() != NavState.ENROUTE) return "車線: 案内準備・再探索中";
        if (LaneFeedService.current() != null) return "車線: 表示可能な推奨車線を受信済み";
        StepInfo step = info.getCurrentStep();
        if (step == null || step.getLanes() == null || step.getLanes().isEmpty())
            return "車線: 案内受信中・現在の案内地点に車線データなし";
        return "車線: データあり・表示可能な推奨方向なし";
    }

    private static void search(final Activity activity, final String query, final boolean avoidTolls) {
        if (preparing) { message(activity, "処理中です。完了してから検索してください"); return; }
        if (query.isEmpty() || query.length() > 200) { message(activity, "目的地を入力してください"); return; }
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 824);
            message(activity, "位置情報を許可してから、もう一度開始してください"); return;
        }
        preparing = true;
        final int ticket = ++generation;
        if (!hasRunningSession()) status = "目的地を確認中";
        new Thread(() -> {
            List<Address> found = null;
            String failure = "";
            try { found = GoogleNavigationClient.searchPlaces(activity.getApplicationContext(), query); }
            catch (Exception error) { failure = error.getMessage(); }
            final String searchFailure = failure;
            final List<Address> choices = found;
            MAIN.post(() -> {
                if (ticket != generation) return;
                preparing = false;
                if (activity.isFinishing() || activity.isDestroyed()) { if (!hasRunningSession()) status = "停止中"; return; }
                if (choices == null || choices.isEmpty()) {
                    String searchStatus = choices == null ? "施設検索に失敗" : "候補が見つかりません";
                    if (!hasRunningSession()) status = searchStatus;
                    new AlertDialog.Builder(activity).setTitle(searchStatus)
                            .setMessage(choices == null ? "API設定・利用上限・通信状態を確認してください。\n" + searchFailure
                                    : "地域名や施設名を追加して検索してください。別の地点を自動選択することはありません。")
                            .setPositiveButton("検索し直す", (d, w) -> show(activity, query))
                            .setNegativeButton("閉じる", null).show(); return;
                }
                String[] labels = new String[choices.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = addressLabel(choices.get(i));
                final int[] selected = {-1};
                final AlertDialog candidates = new AlertDialog.Builder(activity).setTitle("Google Maps：候補 " + labels.length + "件")
                        .setSingleChoiceItems(labels, -1, (dialog, which) -> {
                            selected[0] = which;
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        })
                        .setPositiveButton("車ナビを開始", (dialog, which) -> {
                            if (ticket == generation && selected[0] >= 0) confirmDestination(activity, choices.get(selected[0]), avoidTolls, ticket);
                        }).setNeutralButton("検索し直す", (dialog, which) -> show(activity, query))
                        .setNegativeButton("キャンセル", (dialog, which) -> { if (!hasRunningSession()) status = "停止中"; }).create();
                candidates.show();
                candidates.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            });
        }, "SdkDestinationLookup").start();
    }

    private static boolean hasRunningSession() {
        return session != null && session.state() != NavigationSession.State.STOPPED
                && session.state() != NavigationSession.State.FAILED;
    }

    private static void confirmDestination(final Activity activity, final Address address,
            final boolean avoidTolls, final int ticket) {
        if (ticket != generation || activity.isFinishing() || activity.isDestroyed()) return;
        if (!hasRunningSession()) { initialize(activity, address, avoidTolls); return; }
        new AlertDialog.Builder(activity).setTitle("目的地を変更")
                .setMessage("現在の案内を終了して新しい目的地へ切り替えますか？\n\n" + addressLabel(address)
                        + "\n\n切り替え後に経路取得が失敗した場合、元の案内は自動再開しません。")
                .setPositiveButton("切り替える", (dialog, which) -> {
                    // A stop or a newer search invalidates this confirmation. Never revive an old choice.
                    if (ticket != generation || activity.isFinishing() || activity.isDestroyed()) return;
                    stop(); // Cancels the old request, guidance and lane feed before starting the new session.
                    initialize(activity, address, avoidTolls);
                })
                .setNegativeButton("現在の案内を続ける", null).show();
    }

    private static String addressLabel(Address address) {
        String line = address.getAddressLine(0);
        if (line != null && !line.trim().isEmpty()) return line;
        StringBuilder label = new StringBuilder();
        for (String part : new String[]{address.getAdminArea(), address.getLocality(),
                address.getSubLocality(), address.getThoroughfare(), address.getFeatureName()}) {
            if (part != null && !part.trim().isEmpty()) label.append(part).append(' ');
        }
        if (address.hasLatitude() && address.hasLongitude()) label.append(String.format(Locale.JAPAN,
                "（緯度 %.5f / 経度 %.5f）", address.getLatitude(), address.getLongitude()));
        return label.length() > 0 ? label.toString() : "住所情報なし";
    }

    private static void initialize(final Activity activity, final Address address, final boolean avoidTolls) {
        if (preparing) return;
        if (session != null && session.hasPendingRequest()) {
            // The old SDK completion may clear destinations: drain it before creating a new owner.
            final int waitingTicket = generation;
            final long deadline = SystemClock.elapsedRealtime() + 30000;
            preparing = true;
            MAIN.post(new Runnable() {
                @Override public void run() {
                    if (waitingTicket != generation) return;
                    if (activity.isFinishing() || activity.isDestroyed()) { preparing = false; return; }
                    if (session.hasPendingRequest()) {
                        if (SystemClock.elapsedRealtime() >= deadline) {
                            preparing = false;
                            message(activity, "前の経路処理の終了待ちです。少し待って再度選択してください");
                        } else MAIN.postDelayed(this, 100);
                        return;
                    }
                    preparing = false;
                    initialize(activity, address, avoidTolls);
                }
            });
            return;
        }
        preparing = true;
        final int ticket = ++generation;
        try {
            if (!keyInitialized) {
                String key = MapsCredentialStore.read(activity);
                if (key.isEmpty()) throw new IllegalStateException("missing key");
                NavigationApi.setApiKey(key); keyInitialized = true;
            }
            status = "SDK初期化・利用規約確認中";
            NavigationApi.getNavigator(activity, new NavigationApi.NavigatorListener() {
                @Override public void onNavigatorReady(Navigator ready) {
                    if (ticket != generation) return;
                    preparing = false;
                    if (activity.isFinishing() || activity.isDestroyed()) { status = "停止中"; return; }
                    if (navigator != null && arrivalListener != null) navigator.removeArrivalListener(arrivalListener);
                    navigator = ready;
                    arrivalListener = event -> {
                        // STOPPED can reach the progress feed just before the arrival callback.
                        // The SDK's final-destination event is authoritative for completion.
                        if (!event.isFinalDestination() || !owned || session == null
                                || sessionId.isEmpty() || completedUntil != 0) return;
                        stop();
                        completedUntil = SystemClock.elapsedRealtime() + 8000;
                        MAIN.removeCallbacks(PUBLISH);
                        PUBLISH.run();
                        android.util.Log.i("LokiSdkNav", "final_destination_arrived");
                    };
                    ready.addArrivalListener(arrivalListener);
                    session = SdkNavigationSession.create(activity, ready,
                            new RoutingOptions().travelMode(RoutingOptions.TravelMode.DRIVING)
                                    .avoidTolls(avoidTolls).locationTimeoutMs(20000),
                            () -> BuildConfig.NAVIGATION_SDK_ENABLED);
                    try {
                        destination = addressLabel(address);
                        Waypoint waypoint = Waypoint.builder().setLatLng(address.getLatitude(), address.getLongitude())
                                .setTitle(destination).build();
                        NavigationSession.StartResult result = session.start(Collections.singletonList(waypoint));
                        if (result != NavigationSession.StartResult.STARTED) { status = "開始不可: " + result; message(activity, status); return; }
                        owned = true; sessionId = UUID.randomUUID().toString(); route = new JSONArray();
                        completedUntil = 0;
                        MAIN.removeCallbacks(PUBLISH); PUBLISH.run();
                        message(activity, "SDK経路を取得しています");
                    } catch (Exception failure) { stop(); message(activity, "SDKナビを開始できませんでした"); }
                }
                @Override public void onError(int code) {
                    if (ticket != generation) return;
                    preparing = false; status = "SDK初期化エラー: " + code;
                    message(activity, status);
                }
            });
        } catch (Exception failure) {
            preparing = false; status = "キー設定を確認してください"; message(activity, status);
        }
    }

    public static void stop() {
        ++generation; preparing = false;
        if (session != null) session.stop();
        route = new JSONArray(); status = "停止中";
        if (owned) { MAIN.removeCallbacks(PUBLISH); PUBLISH.run(); }
    }

    public static void stopAndRelease() {
        stop(); owned = false; published = null; MAIN.removeCallbacks(PUBLISH);
        if (navigator != null && arrivalListener != null) navigator.removeArrivalListener(arrivalListener);
        arrivalListener = null;
    }

    private static final Runnable PUBLISH = new Runnable() {
        @Override public void run() {
            if (!owned) return;
            try {
                NavigationSession.State state = session.state();
                NavInfo info = LaneFeedService.navInfo();
                boolean completed = SystemClock.elapsedRealtime() < completedUntil;
                if (info != null && info.getNavState() == NavState.STOPPED && state == NavigationSession.State.ACTIVE) {
                    session.stop(); state = session.state();
                }
                boolean enroute = state == NavigationSession.State.ACTIVE && info != null && info.getNavState() == NavState.ENROUTE;
                boolean active = state == NavigationSession.State.ACTIVE || state == NavigationSession.State.REQUESTING;
                status = state == NavigationSession.State.REQUESTING ? "経路取得中"
                        : enroute ? "案内中" : state == NavigationSession.State.ACTIVE ? "再探索・案内受信待ち"
                        : state == NavigationSession.State.FAILED ? "経路取得に失敗" : "停止中";
                StepInfo step = enroute ? info.getCurrentStep() : null;
                if (!enroute) route = new JSONArray();
                else if (route.length() < 2 || info.getRouteChanged()) {
                    // Geometry failure must not suppress valid turn/lane guidance.
                    try { route = routePoints(); }
                    catch (Exception geometryFailure) { route = new JSONArray(); }
                }
                boolean routeReady = enroute && route.length() > 1;
                String routeStatus = !active ? "stopped" : !enroute ? "fetching"
                        : routeReady ? "ready" : "geometry_pending";
                String lane = laneText();
                String diagnostic = "state=" + state + " feed=" + (info != null)
                        + " enroute=" + enroute + " routePoints=" + route.length()
                        + " laneVisible=" + (enroute && !lane.isEmpty());
                if (!diagnostic.equals(lastDiagnostic)) {
                    android.util.Log.i("LokiSdkNav", diagnostic);
                    lastDiagnostic = diagnostic;
                }
                JSONObject data = new JSONObject().put("ok", true).put("source", "navigation_sdk")
                        .put("sdkSessionId", sessionId).put("time", System.currentTimeMillis())
                        .put("navigationComplete", completed).put("completionId", sessionId)
                        .put("navigationActive", active).put("mapProvider", "google").put("routeMode", "driving")
                        .put("routeDestination", destination).put("destination", destination)
                        .put("routeReady", routeReady).put("route", route)
                        .put("routeStatus", routeStatus)
                        .put("instruction", step != null && step.getFullInstructionText() != null ? step.getFullInstructionText() : "SDK: " + status)
                        .put("detail", "Google Maps SDK").put("nextDistance", enroute ? distance(info.getDistanceToCurrentStepMeters()) : "")
                        .put("totalRemainingDistance", enroute ? distance(info.getDistanceToFinalDestinationMeters()) : "")
                        .put("totalRemainingDuration", enroute ? duration(info.getTimeToFinalDestinationSeconds()) : "")
                        .put("sdkLaneText", enroute ? lane : "")
                        .put("sdkLaneSequence", LaneFeedService.sequence())
                        .put("sdkLaneStatus", laneStatus())
                        .put("sdkLaneTtlMs", LaneFeedService.current() == null ? 0 : Math.max(0,
                                5000 - (SystemClock.elapsedRealtime() - LaneFeedService.current().receivedElapsedMs)));
                published = data.toString(); publishedAt = SystemClock.elapsedRealtime();
            } catch (Exception failure) {
                // Retain ownership but expire the payload; never fall back to another trip.
                publishedAt = 0;
            }
            MAIN.postDelayed(this, 1000);
        }
    };

    private static JSONArray routePoints() throws Exception {
        ArrayList<double[]> points = new ArrayList<>();
        List<RouteSegment> segments = navigator.getRouteSegments();
        if (segments != null) for (RouteSegment segment : segments) {
            if (segment == null || segment.getLatLngs() == null || segment.getLatLngs().isEmpty())
                return new JSONArray(); // Never bridge a missing segment with a made-up straight line.
            for (LatLng p : segment.getLatLngs()) {
                if (p == null || !Double.isFinite(p.latitude) || !Double.isFinite(p.longitude)
                        || Math.abs(p.latitude) > 90 || Math.abs(p.longitude) > 180) return new JSONArray();
                if (!points.isEmpty()) {
                    double[] last = points.get(points.size() - 1);
                    if (last[0] == p.latitude && last[1] == p.longitude) continue;
                }
                points.add(new double[]{p.latitude, p.longitude});
            }
        }
        double[][] all = points.toArray(new double[0][]);
        JSONArray out = new JSONArray();
        for (int index : RouteGeometry.indices(all, 4096)) out.put(new JSONArray().put(all[index][0]).put(all[index][1]));
        return out;
    }

    private static String laneText() {
        LaneSnapshot lanes = LaneFeedService.current();
        if (lanes == null || lanes.lanes.size() > 10) return "";
        StringBuilder text = new StringBuilder("車線（●推奨） ");
        for (List<LaneSnapshot.Direction> lane : lanes.lanes) {
            text.append('[');
            for (LaneSnapshot.Direction d : lane) {
                String[] symbols = {"?", "↑", "↖", "↗", "←", "→", "↙", "↘", "↶", "↷"};
                String symbol = d.sdkShape >= 0 && d.sdkShape < symbols.length ? symbols[d.sdkShape] : "?";
                if (d.recommended && !"?".equals(symbol)) text.append('●');
                text.append(symbol);
            }
            text.append("] ");
        }
        return text.length() <= 100 ? text.toString() : "";
    }
    private static String distance(Integer meters) {
        if (meters == null || meters < 0) return "";
        return meters < 1000 ? meters + "m" : String.format(Locale.JAPAN, "%.1fkm", meters / 1000.0);
    }
    private static String duration(Integer seconds) { return seconds == null || seconds < 0 ? "" : ((seconds + 59) / 60) + "分"; }
    private static void message(Activity activity, String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
}
