package com.example.rokidgeminisecretary;

public final class TopicModeTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        check(TopicMode.IDS.length == TopicMode.LABELS.length);
        for (String id : TopicMode.IDS) {
            check(TopicMode.valid(id)); check(id.equals(TopicMode.normalize(id)));
            check(TopicMode.audio(id) == ("sound".equals(id) || "both".equals(id)));
            check(TopicMode.camera(id) == ("vision".equals(id) || "both".equals(id)));
            if (!"scheduled".equals(id)) {
                check(TopicMode.interval(id, 3) == 8000L);
                check(TopicMode.interval(id, 30) == 8000L);
            }
        }
        check(TopicMode.interval("scheduled", 3) == 180000L);
        check(TopicMode.interval("scheduled", 10) == 600000L);
        check(TopicMode.interval("scheduled", 30) == 1800000L);
        check(TopicMode.interval("scheduled", -1) == 600000L);
        check(TopicMode.interval("unknown", 3) == 180000L);
        check(!TopicMode.valid(null)); check(!TopicMode.valid("off"));
        check("scheduled".equals(TopicMode.normalize(null)));
        check(!TopicMode.audio("continuous") && !TopicMode.camera("continuous"));
        check(!TopicMode.audio("scheduled") && !TopicMode.camera("scheduled"));
        System.out.println("TopicModeTest: " + checks + " checks passed");
    }
}
