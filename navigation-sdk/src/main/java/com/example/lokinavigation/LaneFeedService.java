package com.example.lokinavigation;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager;
import com.google.android.libraries.mapsplatform.turnbyturn.model.Lane;
import com.google.android.libraries.mapsplatform.turnbyturn.model.LaneDirection;
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo;
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavState;
import com.google.android.libraries.mapsplatform.turnbyturn.model.StepInfo;
import com.google.android.libraries.navigation.Navigator;
import java.util.ArrayList;
import java.util.List;

/** Receives an already-initialized phone Navigator; does not start or bill navigation. */
public final class LaneFeedService extends Service {
    private static volatile LaneSnapshot latest;
    private static volatile boolean accepting;
    private static volatile NavInfo raw;
    private static volatile long rawAt;
    private static volatile long sequence;
    public static long sequence() { return sequence; }

    public static NavInfo navInfo() {
        return accepting && SystemClock.elapsedRealtime() - rawAt <= LaneSnapshot.MAX_AGE_MS ? raw : null;
    }
    private Messenger messenger;

    public static boolean attach(Navigator navigator, String packageName) {
        latest = null;
        raw = null;
        accepting = true;
        try {
            boolean registered = navigator.registerServiceForNavUpdates(packageName, LaneFeedService.class.getName(), 1);
            android.util.Log.d("LokiLaneFeed", "attach registered=" + registered);
            if (!registered) accepting = false;
            return registered;
        } catch (RuntimeException failure) {
            accepting = false;
            throw failure;
        }
    }

    public static void detach(Navigator navigator) {
        android.util.Log.d("LokiLaneFeed", "detach");
        accepting = false;
        raw = null;
        try { navigator.unregisterServiceForNavUpdates(); }
        finally { latest = null; }
    }

    /** Caller must also expire the rendered HUD after MAX_AGE_MS if transport disconnects. */
    public static LaneSnapshot current() {
        LaneSnapshot value = latest;
        return accepting && value != null && value.displayable(SystemClock.elapsedRealtime()) ? value : null;
    }

    @Override public void onCreate() {
        super.onCreate();
        final TurnByTurnManager manager = TurnByTurnManager.createInstance();
        messenger = new Messenger(new Handler(Looper.getMainLooper()) {
            @Override public void handleMessage(Message message) {
                if (!accepting || message.what != TurnByTurnManager.MSG_NAV_INFO) return;
                try {
                    raw = manager.readNavInfoFromBundle(message.getData());
                    rawAt = SystemClock.elapsedRealtime();
                    latest = convert(raw);
                    sequence++;
                    StepInfo step = raw == null ? null : raw.getCurrentStep();
                    android.util.Log.d("LokiLaneFeed", "rx seq=" + sequence
                            + " state=" + (raw == null ? -1 : raw.getNavState())
                            + " step=" + (step != null)
                            + " lanes=" + (step == null || step.getLanes() == null ? 0 : step.getLanes().size())
                            + " recommended=" + (latest != null && latest.displayable(rawAt)));
                } catch (RuntimeException invalidFeed) {
                    latest = null; raw = null;
                    android.util.Log.d("LokiLaneFeed", "rx invalid=" + invalidFeed.getClass().getSimpleName());
                }
            }
        });
    }

    static LaneSnapshot convert(NavInfo info) {
        if (info == null) return null;
        LaneSnapshot.State state = LaneSnapshot.State.UNKNOWN;
        if (info.getNavState() == NavState.ENROUTE) state = LaneSnapshot.State.ENROUTE;
        else if (info.getNavState() == NavState.REROUTING) state = LaneSnapshot.State.REROUTING;
        else if (info.getNavState() == NavState.STOPPED) state = LaneSnapshot.State.STOPPED;
        ArrayList<List<LaneSnapshot.Direction>> lanes = new ArrayList<>();
        StepInfo step = state == LaneSnapshot.State.ENROUTE ? info.getCurrentStep() : null;
        if (step != null && step.getLanes() != null) for (Lane lane : step.getLanes()) {
            ArrayList<LaneSnapshot.Direction> directions = new ArrayList<>();
            if (lane != null && lane.laneDirections() != null) for (LaneDirection direction : lane.laneDirections()) {
                if (direction != null) directions.add(new LaneSnapshot.Direction(direction.laneShape(),
                        Boolean.TRUE.equals(direction.isRecommended())));
            }
            lanes.add(directions);
        }
        return new LaneSnapshot(state, SystemClock.elapsedRealtime(), info.getDistanceToCurrentStepMeters(), lanes);
    }

    @Override public IBinder onBind(Intent intent) { android.util.Log.d("LokiLaneFeed", "bind"); return messenger.getBinder(); }
    @Override public boolean onUnbind(Intent intent) { android.util.Log.d("LokiLaneFeed", "unbind"); latest = null; raw = null; return false; }
    @Override public void onDestroy() { latest = null; raw = null; super.onDestroy(); }
}
