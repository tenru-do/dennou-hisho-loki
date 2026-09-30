package com.example.rokidgeminisecretary;
final class RoutesRequestPolicy {
    static String mode(String value) {
        if ("walking".equalsIgnoreCase(value) || "WALK".equalsIgnoreCase(value)) return "WALK";
        if ("bicycling".equalsIgnoreCase(value) || "BICYCLE".equalsIgnoreCase(value)) return "BICYCLE";
        if ("transit".equalsIgnoreCase(value)) return "TRANSIT";
        if ("driving".equalsIgnoreCase(value) || "DRIVE".equalsIgnoreCase(value)) return "DRIVE";
        throw new IllegalArgumentException("google_invalid_mode");
    }
    static String mask(String mode) {
        if ("TRANSIT".equals(mode)) return "routes.legs,routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.description,routes.warnings";
        String basic = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,"
                + "routes.legs.steps.startLocation,routes.legs.steps.distanceMeters,"
                + "routes.legs.steps.staticDuration,routes.legs.steps.navigationInstruction";
        if (!"DRIVE".equals(mode)) basic += ",routes.legs.steps.travelMode,routes.legs.steps.polyline.encodedPolyline,routes.legs.steps.endLocation";
        return basic;
    }
    static String redact(String text, String key, String destination) {
        if (text == null) return "";
        if (key != null && !key.isEmpty()) text = text.replace(key, "[KEY]");
        if (destination != null && !destination.isEmpty()) text = text.replace(destination, "[DESTINATION]");
        return text.replaceAll("AIza[\\w-]+", "[KEY]")
                .replaceAll("(?i)(key|token|authorization)[=:\\s]+[^\\s,\\\"}]+", "$1=[REDACTED]")
                .replaceAll("https?://[^\\s\\\"]+", "[URL]")
                .replaceAll("-?\\d{1,3}\\.\\d{4,}", "[COORD]");
    }
}
