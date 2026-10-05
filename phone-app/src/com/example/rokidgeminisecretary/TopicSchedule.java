package com.example.rokidgeminisecretary;

/** News collection only; live personal data is fetched at playback. */
final class TopicSchedule {
    static int dueSlot(int hour, int minute) {
        if (minute < 0 || minute >= 15) return -1;
        return hour == 7 || hour == 12 || hour == 17 || hour == 21 ? hour : -1;
    }
}
