package com.example.lokinavigation;

import java.time.LocalDate;

/** Conservative per-installation limits, not an account-wide billing guarantee. */
public final class DestinationQuota {
    public static final int DAILY_LIMIT = 10;
    public static final int MONTHLY_LIMIT = 300;
    public final LocalDate day;
    public final int daily;
    public final int monthly;

    public DestinationQuota(LocalDate day, int daily, int monthly) {
        if (day == null || daily < 0 || monthly < daily) throw new IllegalArgumentException("Invalid quota state");
        this.day = day;
        this.daily = daily;
        this.monthly = monthly;
    }

    /** Null means deny. Moving the device clock backwards must not reset counters. */
    public DestinationQuota reserve(LocalDate today, int destinations) {
        if (today == null || today.isBefore(day) || destinations <= 0 || destinations > DAILY_LIMIT) return null;
        int usedDay = today.equals(day) ? daily : 0;
        int usedMonth = today.getYear() == day.getYear() && today.getMonth() == day.getMonth() ? monthly : 0;
        if (usedDay > DAILY_LIMIT - destinations || usedMonth > MONTHLY_LIMIT - destinations) return null;
        return new DestinationQuota(today, usedDay + destinations, usedMonth + destinations);
    }
}
