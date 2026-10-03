package com.example.rokidgeminisecretary;

/** Stable identifiers shared by phone controls and the glass TOPIC menu. */
final class TopicMode {
    static final String[] IDS = {"scheduled", "continuous", "sound", "vision", "both"};
    static final String[] LABELS = {"定期ラジオ", "連続トーク", "周囲の音", "カメラ", "音＋カメラ"};
    static boolean valid(String mode) {
        for (String id : IDS) if (id.equals(mode)) return true;
        return false;
    }
    static String normalize(String mode) { return valid(mode) ? mode : "scheduled"; }
    static boolean audio(String mode) { return "sound".equals(mode) || "both".equals(mode); }
    static boolean camera(String mode) { return "vision".equals(mode) || "both".equals(mode); }
    static long interval(String mode, int minutes) {
        return "scheduled".equals(normalize(mode)) ? (minutes == 3 || minutes == 30 ? minutes : 10) * 60000L : 8000L;
    }
}
