package com.example.rokidgeminisecretary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NavigationLabels {
    static String clean(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cf}\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
    }
    static String destination(String... values) {
        for (String value : values) {
            String text = clean(value);
            Matcher m = Pattern.compile("^(?:目的地|行き先)\\s*[:：]\\s*(.+)$").matcher(text);
            if (m.find()) return m.group(1).trim();
            m = Pattern.compile("^(.+?)(?:へのナビ|への経路|に向かっています|へ向かっています)$").matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        return "";
    }
    static String mode(String text) {
        if (text.contains("自転車") || text.contains("Cycling")) return "bicycling";
        if (text.contains("徒歩") || text.contains("Walking")) return "walking";
        if (text.contains("電車") || text.contains("番線") || text.contains("乗換") || text.contains("乗り換え")) return "transit";
        if (text.contains("車で") || text.contains("Driving")) return "driving";
        return "";
    }
    static String accessMode(String instruction) {
        String text = clean(instruction);
        if (text.matches(".*(?:自転車|Cycling|Bicycling).*")) return "bicycling";
        if (text.matches(".*(?:徒歩|Walking).*")) return "walking";
        return "";
    }
    /** Access is not the final destination. Require access mode and a station departure. */
    static String transitAccessStation(String instruction, String detail) {
        String first = clean(instruction), second = clean(detail);
        if (accessMode(first).isEmpty()) return "";
        Matcher m = Pattern.compile("^(.{1,60}?(?:駅|バス停))\\s*[·・|｜]\\s*(?:午前|午後|AM|PM)?\\s*\\d{1,2}[:：]\\d{2}\\s*発(?:\\s.*)?$").matcher(second);
        return m.matches() ? m.group(1).trim() : "";
    }
}
