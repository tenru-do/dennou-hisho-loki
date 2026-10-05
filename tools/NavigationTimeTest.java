package com.example.rokidgeminisecretary;

import java.util.Calendar;
import java.util.TimeZone;

public final class NavigationTimeTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Tokyo");
    private static long at(int hour, int minute) {
        Calendar c = Calendar.getInstance(ZONE);
        c.clear(); c.set(2026, 8, 20, hour, minute, 0);
        return c.getTimeInMillis();
    }
    private static void check(String s, int h, int m, int minutes) {
        long value = NavigationTime.remainingMillis(s, at(h,m), ZONE);
        if (minutes < 0 ? value != -1 : value != minutes * 60000L)
            throw new AssertionError(s + " remaining=" + value);
    }
    public static void main(String[] args) {
        check("午後11:17 着",23,12,5);
        check("午前7:53 着",7,41,12);
        check("23:17 到着",23,12,5);
        check("到着 午後1:05",12,55,10);
        check("午後12:05 着",11,55,10);
        check("午前12:05 着",23,55,10);
        check("0:05 着",23,55,10);
        check("ETA 11:17 PM",23,12,5);
        check("AM 7:53 arrival",7,41,12);
        check("午後7:53 着",23,12,-1);
        check("午後13:05 着",12,55,-1);
        check("7:53 着",12,30,-1);
        check("北東に進む",12,30,-1);
        System.out.println("13 navigation arrival time checks passed");
    }
}
