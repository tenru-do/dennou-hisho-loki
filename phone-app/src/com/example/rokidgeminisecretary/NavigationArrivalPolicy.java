package com.example.rokidgeminisecretary;

/** Conservative completion signal for an independently calculated Google route. */
final class NavigationArrivalPolicy {
    static boolean destinationArrivalMessage(String instruction) {
        String text = instruction == null ? "" : instruction.trim();
        // Require final-destination wording. Station arrivals and ETA clocks
        // are intermediate guidance, not trip completion.
        return "目的地に到着しました".equals(text) || "目的地に到着".equals(text)
                || "目的地に到着しました。".equals(text)
                || "You've arrived".equalsIgnoreCase(text)
                || "You have arrived".equalsIgnoreCase(text);
    }
    static boolean nearFinalPoint(long now, long locationAt, float accuracyMeters,
                                  float speedMetersPerSecond, long routeAt, float distanceMeters) {
        return locationAt > 0 && now >= locationAt && now - locationAt <= 15000
                && accuracyMeters > 0 && accuracyMeters <= 35
                // Android may omit speed while stationary. The caller also
                // requires sustained proximity; missing speed must not block arrival.
                && (speedMetersPerSecond == -1 || (speedMetersPerSecond >= 0 && speedMetersPerSecond <= 2.5f))
                && routeAt > 0 && now - routeAt >= 30000
                && distanceMeters >= 0 && distanceMeters <= 30;
    }
}
