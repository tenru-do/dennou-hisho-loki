package com.example.rokidkeyboardbridge;

final class NavigationHudText {
    static String instruction(String primary, String secondary, String distance) {
        primary = primary == null ? "" : primary.trim();
        secondary = secondary == null ? "" : secondary.trim();
        distance = distance == null ? "" : distance.trim();
        String chosen = primary;
        if (action(secondary) && !action(primary)) chosen = secondary;
        if (chosen.length() == 0) chosen = secondary;
        if (distance.length() > 0 && chosen.startsWith(distance)) {
            chosen = chosen.substring(distance.length()).replaceFirst("^[\\s・·,、:：\\-]+", "").trim();
        }
        if (chosen.length() == 0) chosen = secondary;
        if ("Googleマップ画面共有".equals(chosen) || "MAP ナビゲーション".equals(chosen)) return "";
        return chosen;
    }

    private static boolean action(String value) {
        return value.matches("(?is).*(?:進む|直進|右折|左折|曲が|方向|方面|乗換|乗り換え|下車|降り|乗車|出口|到着しました|turn|continue|head |exit |toward).*" );
    }
}
