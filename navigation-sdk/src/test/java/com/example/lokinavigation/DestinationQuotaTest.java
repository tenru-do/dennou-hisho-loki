package com.example.lokinavigation;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;

public class DestinationQuotaTest {
    private final LocalDate day = LocalDate.of(2026, 9, 27);
    @Test public void dailyBoundary() {
        DestinationQuota q = new DestinationQuota(day, 9, 25);
        DestinationQuota last = q.reserve(day, 1);
        assertEquals(10, last.daily);
        assertNull(last.reserve(day, 1));
        assertNull(q.reserve(day, 2));
    }
    @Test public void monthlyBoundarySurvivesDayChange() {
        DestinationQuota q = new DestinationQuota(day, 0, 299);
        DestinationQuota last = q.reserve(day.plusDays(1), 1);
        assertEquals(300, last.monthly);
        assertNull(last.reserve(day.plusDays(2), 1));
        assertEquals(1, last.reserve(LocalDate.of(2026, 10, 1), 1).monthly);
    }
    @Test public void rejectClockRollbackAndInvalidCounts() {
        DestinationQuota q = new DestinationQuota(day, 0, 0);
        assertNull(q.reserve(day.minusDays(1), 1));
        assertNull(q.reserve(day, 0));
        assertNull(q.reserve(day, -1));
        assertNull(q.reserve(day, Integer.MAX_VALUE));
    }
    @Test public void countEveryWaypoint() {
        DestinationQuota q = new DestinationQuota(day, 1, 2).reserve(day, 4);
        assertEquals(5, q.daily);
        assertEquals(6, q.monthly);
    }
}
