package com.example.rokidkeyboardbridge;

final class NavigationHudText {
    static String instruction(String primary, String secondary, String distance) {
        primary = guidance(primary);
        secondary = guidance(secondary);
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

    static String endEllipsis(String text, int count) {
        if (text == null) return "";
        text = text.replace('\n', ' ').trim();
        if (text.codePointCount(0, text.length()) <= count) return text;
        return text.substring(0, text.offsetByCodePoints(0, Math.max(0, count))) + "…";
    }

    private static String guidance(String text) {
        String value = text == null ? "" : text.trim();
        if (value.matches("(?is)^(?:Google\\s*マップ|Google\\s*Maps?)(?:とは[？?]?|[？?])$")) return "";
        return value;
    }

    private static boolean action(String value) {
        return value.matches("(?is).*(?:進む|直進|右折|左折|曲が|方向|方面|乗換|乗り換え|下車|降り|乗車|出口|到着しました|turn|continue|head |exit |toward).*" );
    }
}
