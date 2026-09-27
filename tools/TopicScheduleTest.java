package com.example.rokidgeminisecretary;
public final class TopicScheduleTest {
    public static void main(String[] args) {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                boolean expected = (hour == 7 || hour == 12 || hour == 17 || hour == 21) && minute < 15;
                if ((TopicSchedule.dueSlot(hour, minute) >= 0) != expected)
                    throw new AssertionError(hour + ":" + minute);
            }
        }
        System.out.println("1440 topic schedule cases passed");
    }
}
