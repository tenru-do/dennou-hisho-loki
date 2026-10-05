package com.example.rokidgeminisecretary;
public class RoutesRequestPolicyTest {
    public static void main(String[] args) {
        for (String mode : new String[]{"WALK", "BICYCLE", "DRIVE"}) {
            if (RoutesRequestPolicy.mask(mode).contains("transit")) throw new AssertionError(mode);
            if (!RoutesRequestPolicy.mask(mode).contains("routes.polyline.encodedPolyline")) throw new AssertionError(mode);
        }
        if (!RoutesRequestPolicy.mask("TRANSIT").equals("routes.legs,routes.duration,routes.distanceMeters,routes.polyline,routes.description,routes.warnings")) throw new AssertionError();
        if (!RoutesRequestPolicy.mode("WALK").equals("WALK") || !RoutesRequestPolicy.mode("bicycling").equals("BICYCLE")) throw new AssertionError();
        String safe=RoutesRequestPolicy.redact("Invalid argument secret-key private-place 35.123456 https://example.com/?key=abc", "secret-key", "private-place");
        if (safe.contains("secret-key") || safe.contains("private-place") || safe.contains("35.123456") || safe.contains("https://")) throw new AssertionError(safe);
        if (!safe.contains("Invalid argument")) throw new AssertionError();
        System.out.println("RoutesRequestPolicy checks passed");
    }
}
