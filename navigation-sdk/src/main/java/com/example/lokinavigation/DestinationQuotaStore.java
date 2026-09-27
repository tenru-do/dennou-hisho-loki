package com.example.lokinavigation;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.ZoneId;

/** Reserve before SDK destination requests. Never refund uncertain/failed requests. */
public final class DestinationQuotaStore {
    private DestinationQuotaStore() {}

    public static synchronized boolean reserve(Context context, int destinations) {
        // Fixed zone prevents timezone changes from opening a second daily allowance.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        SharedPreferences prefs = context.getSharedPreferences("loki_navigation_sdk_quota_v1", Context.MODE_PRIVATE);
        try {
            DestinationQuota previous = new DestinationQuota(
                    LocalDate.parse(prefs.getString("day", today.toString())),
                    prefs.getInt("daily", 0), prefs.getInt("monthly", 0));
            DestinationQuota next = previous.reserve(today, destinations);
            if (next == null) return false;
            // Synchronous durable write: never send if the accounting cannot be saved.
            return prefs.edit().putString("day", next.day.toString())
                    .putInt("daily", next.daily).putInt("monthly", next.monthly).commit();
        } catch (RuntimeException invalidState) {
            return false;
        }
    }
}
