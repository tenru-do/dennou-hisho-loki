package com.example.rokidgeminisecretary;

import com.example.rokidkeyboardbridge.RadioTopicBuffer;

public final class RadioPrefetchTest {
    private static int checks;
    private static void check(boolean v) { checks++; if (!v) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        RadioPrefetchQueue<String> phone = new RadioPrefetchQueue<String>();
        check(phone.beginGeneration() == -1);
        check(phone.activate("one", 7, 100));
        long a = phone.beginGeneration(); check(a > 0); check(phone.beginGeneration() == -1);
        check(phone.offer(a, "A", "first")); check(phone.beginGeneration() == -1);
        check("first".equals(phone.poll("one", 7))); check("first".equals(phone.poll("one", 7)));
        check(phone.beginGeneration() == -1); check(!phone.start("one", 7, "wrong"));
        check(phone.start("one", 7, "A")); check(phone.speaking());
        long b = phone.beginGeneration(); check(b > a);
        check(phone.finish("one", 7, "A")); check(!phone.speaking());
        check(phone.offer(b, "B", "second")); check("second".equals(phone.poll("one", 7)));
        check(phone.start("one", 7, "B"));
        long c = phone.beginGeneration(); phone.finishGeneration(b); check(phone.currentWork(c));
        check(phone.cancel("one", 7)); check(!phone.offer(c, "C", "stale"));
        check(phone.poll("one", 7) == null); check(!phone.activate("one", 7, 100));
        check(phone.activate("two", 7, 101)); check(!phone.activate("one", 7, 100));
        check(!phone.cancel("one", 7)); check(phone.matches("two", 7));
        long d = phone.beginGeneration(); phone.clear(); check(!phone.offer(d, "D", "old"));
        check(phone.activate("three", 8, 102)); check(phone.poll("three", 7) == null);
        RadioTopicBuffer<String> glass = new RadioTopicBuffer<String>();
        glass.arm("three", 8, 102); long token = glass.token();
        check(glass.offer("three", 8, "E", 10000, 100, "now")); check("now".equals(glass.take(0)));
        glass.started("E"); check(!glass.offer("three", 8, "E", 10000, 100, "replay"));
        check(glass.offer("three", 8, "F", 10000, 100, "next")); check(glass.take(500) == null);
        glass.finished(2000); check(glass.remaining(2000) == 1800); check(glass.take(3799) == null);
        check("next".equals(glass.take(3800))); check(glass.take(3800) == null);
        check(glass.offer("three", 8, "G", 10000, 100, "unspoken")); glass.finished(4000);
        glass.clear(); check(glass.token() != token); check(glass.take(5800) == null);
        check(!glass.offer("three", 8, "late", 10000, 100, "old callback"));
        glass.arm("four", 9, 103); check(!glass.offer("three", 8, "late", 10000, 100, "old session"));
        check(!glass.offer("four", 9, "H", 100, 100, "expired"));
        check(glass.offer("four", 9, "I", 200, 100, "valid")); check(glass.expired(200));
        System.out.println("RadioPrefetchTest: " + checks + " checks passed");
    }
}
