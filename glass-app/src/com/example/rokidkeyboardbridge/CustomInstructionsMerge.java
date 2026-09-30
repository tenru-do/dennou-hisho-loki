package com.example.rokidkeyboardbridge;

import java.util.HashSet;

/** Preserves the glasses' instructions and appends only new phone lines. */
final class CustomInstructionsMerge {
    private CustomInstructionsMerge() {}

    static String replaceIfCurrent(String current, String expected, String replacement, int maxChars) {
        if (current == null || expected == null || !current.equals(expected)
                || replacement == null || replacement.trim().isEmpty()
                || replacement.length() > maxChars) return null;
        return replacement;
    }

    static String merge(String glass, String phone, int maxChars) {
        String base = glass == null ? "" : glass.trim();
        String incoming = phone == null ? "" : phone.trim();
        if (incoming.isEmpty()) return base;
        StringBuilder result = new StringBuilder(base);
        HashSet<String> known = new HashSet<String>();
        for (String line : base.split("\\r?\\n")) known.add(line.trim());
        for (String line : incoming.split("\\r?\\n")) {
            String addition = line.trim();
            if (addition.isEmpty() || !known.add(addition)) continue;
            if (result.length() > 0) result.append('\n');
            result.append(addition);
            if (result.length() > maxChars) return null;
        }
        return result.toString();
    }
}
