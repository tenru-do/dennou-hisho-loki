package com.example.rokidgeminisecretary;

/** Credentials are opaque: Google, not a prefix/length regex, validates them. */
final class MapsKeyInput {
    static String normalize(String input) {
        String value = trimBoundary(input == null ? "" : input);
        if (value.length() >= 2) {
            char first = value.charAt(0), last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')
                    || (first == '\u201c' && last == '\u201d')) {
                value = trimBoundary(value.substring(1, value.length() - 1));
            }
        }
        return value;
    }
    private static String trimBoundary(String value) {
        int start = 0, end = value.length();
        while (start < end && boundary(value.charAt(start))) start++;
        while (end > start && boundary(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean boundary(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\ufeff' || c == '\u200b';
    }
    static boolean canSend(String value) {
        if (value == null || value.length() == 0 || value.length() > 512) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 33 || c > 126) return false;
        }
        return true;
    }
}
