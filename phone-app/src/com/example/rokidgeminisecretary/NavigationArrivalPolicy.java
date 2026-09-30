package com.example.rokidgeminisecretary;

/** Conservative completion signal for an independently calculated Google route. */
final class NavigationArrivalPolicy {
    static boolean nearFinalPoint(long now, long locationAt, float accuracyMeters,
                                  float speedMetersPerSecond, long routeAt, float distanceMeters) {
        return locationAt > 0 && now >= locationAt && now - locationAt <= 15000
                && accuracyMeters > 0 && accuracyMeters <= 35
                && speedMetersPerSecond >= 0 && speedMetersPerSecond <= 2.5f
                && routeAt > 0 && now - routeAt >= 30000
                && distanceMeters >= 0 && distanceMeters <= 30;
    }
}
