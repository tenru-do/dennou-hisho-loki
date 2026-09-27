package com.example.lokinavigation;

import android.content.Context;
import android.os.Looper;
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoutingOptions;
import com.google.android.libraries.navigation.Waypoint;
import java.util.List;

/** Connects the tested controller to a host-owned, initialized phone Navigator. */
public final class SdkNavigationSession {
    private SdkNavigationSession() {}

    public static NavigationSession<Waypoint> create(Context context, final Navigator navigator,
            final RoutingOptions routing, NavigationSession.Gate gate) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Main thread required");
        final Context app = context.getApplicationContext();
        return new NavigationSession<>(gate, new NavigationSession.Quota() {
            @Override public boolean reserve(int count) { return DestinationQuotaStore.reserve(app, count); }
        }, new NavigationSession.Engine<Waypoint>() {
            @Override public void request(List<Waypoint> destinations, final NavigationSession.Completion callback) {
                navigator.setDestinations(destinations, routing).setOnResultListener(
                        new ListenableResultFuture.OnResultListener<Navigator.RouteStatus>() {
                            @Override public void onResult(Navigator.RouteStatus status) {
                                callback.complete(status == Navigator.RouteStatus.OK);
                            }
                        });
            }
            @Override public boolean startGuidanceAndFeed() {
                if (!LaneFeedService.attach(navigator, app.getPackageName())) return false;
                navigator.startGuidance();
                return true;
            }
            @Override public void stopAndClear() {
                // Clear the feed first, even if SDK stop/clear subsequently throws.
                try { LaneFeedService.detach(navigator); }
                finally {
                    try { navigator.stopGuidance(); }
                    finally { navigator.clearDestinations(); }
                }
            }
        });
    }
}
