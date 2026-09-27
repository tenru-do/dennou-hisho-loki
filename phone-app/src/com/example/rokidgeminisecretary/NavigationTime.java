package com.example.rokidgeminisecretary;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NavigationTime {
    private static final Pattern CLOCK = Pattern.compile(
            "(?<![0-9])(午前|午後|AM|PM)?\\s*([01]?[0-9]|2[0-3])\\s*[:：]\\s*([0-5][0-9])\\s*(AM|PM)?",
            Pattern.CASE_INSENSITIVE);

    static long remainingMillis(String text, long now, TimeZone zone) {
        if (text == null || !text.matches("(?is).*(?:着|到着|\\bETA\\b|arrival).*")) return -1L;
        Matcher clock = CLOCK.matcher(text);
        int hour = -1, minute = -1;
        while (clock.find()) {
            hour = Integer.parseInt(clock.group(2));
            minute = Integer.parseInt(clock.group(3));
            String period = clock.group(1) != null ? clock.group(1) : clock.group(4);
            if (period != null) {
                if (hour > 12) return -1L;
                period = period.toUpperCase(Locale.ROOT);
                hour %= 12;
                if ("午後".equals(period) || "PM".equals(period)) hour += 12;
            }
        }
        if (hour < 0) return -1L;
        Calendar current = Calendar.getInstance(zone);
        current.setTimeInMillis(now);
        Calendar arrival = (Calendar) current.clone();
        arrival.set(Calendar.HOUR_OF_DAY, hour);
        arrival.set(Calendar.MINUTE, minute);
        arrival.set(Calendar.SECOND, 0);
        arrival.set(Calendar.MILLISECOND, 0);
        if (arrival.getTimeInMillis() < now - 120000L) {
            // Only a plausible overnight arrival may move to tomorrow.
            if (current.get(Calendar.HOUR_OF_DAY) < 18 || hour > 6) return -1L;
            arrival.add(Calendar.DAY_OF_MONTH, 1);
        }
        long remaining = arrival.getTimeInMillis() - now;
        return remaining > 0 && remaining <= 8L * 3600000L ? remaining : -1L;
    }
}
