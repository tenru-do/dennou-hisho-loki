package com.example.rokidgeminisecretary;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import java.util.Locale;

/** Two explicit, device-local destination choices. No API key or route is persisted. */
final class DestinationPresets {
    static final class Place {
        final String label, placeId;
        final double latitude, longitude;
        Place(String label, double latitude, double longitude, String placeId) {
            this.label = label; this.latitude = latitude; this.longitude = longitude;
            this.placeId = placeId == null ? "" : placeId;
        }
        Address address() {
            Address value = new Address(Locale.JAPAN);
            value.setLatitude(latitude); value.setLongitude(longitude);
            value.setFeatureName(label); value.setAddressLine(0, label);
            if (!placeId.isEmpty()) value.setUrl(placeId);
            return value;
        }
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("loki_destination_presets", 0); }
    static Place read(Context c, String kind) {
        SharedPreferences p = prefs(c);
        String label = p.getString(kind + "_label", "");
        if (label.isEmpty()) return null;
        double lat = Double.longBitsToDouble(p.getLong(kind + "_lat", 0));
        double lng = Double.longBitsToDouble(p.getLong(kind + "_lng", 0));
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) return null;
        return new Place(label, lat, lng, p.getString(kind + "_id", ""));
    }
    static boolean save(Context c, String kind, Place place) {
        if (!"home".equals(kind) && !"work".equals(kind)) return false;
        if (place == null || place.label.trim().isEmpty() || place.label.length() > 200
                || !Double.isFinite(place.latitude) || !Double.isFinite(place.longitude)
                || Math.abs(place.latitude) > 90 || Math.abs(place.longitude) > 180) return false;
        return prefs(c).edit().putString(kind + "_label", place.label.trim())
                .putLong(kind + "_lat", Double.doubleToRawLongBits(place.latitude))
                .putLong(kind + "_lng", Double.doubleToRawLongBits(place.longitude))
                .putString(kind + "_id", place.placeId).commit();
    }
}
