package com.example.rokidgeminisecretary;

/** Queue boundary: preserve body/newlines while removing the repeated column opening. */
final class ColumnSpeechFormatter {
    static String format(String text, boolean continuous) {
        if (text == null) return "";
        if (!continuous) return text;
        return text.replaceFirst("^(?:マスター[、,\\s]*)?(?:\\d{1,2}月\\d{1,2}日[\\s、,]*[月火水木金土日]曜日[\\s、,]*)?(?:朝|昼|夜)の知識コラムです[。．]", "");
    }
    static boolean accepts(String text) {
        return text != null && text.length() >= 400 && text.length() <= 600;
    }
}
