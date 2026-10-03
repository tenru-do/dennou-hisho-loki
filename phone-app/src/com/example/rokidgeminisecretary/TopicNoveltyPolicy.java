package com.example.rokidgeminisecretary;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Rejects verbatim and strongly overlapping scripts in addition to prompt-level semantic exclusions. */
final class TopicNoveltyPolicy {
    static boolean repeatedFacts(int matched, int total, boolean sameTopic) {
        return matched > 0 && (sameTopic || matched * 2 >= total);
    }
    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    static boolean duplicate(String proposed, String previous) {
        String a = normalize(proposed), b = normalize(previous);
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.equals(b)) return true;
        if (Math.min(a.length(), b.length()) < 20) return false;
        Set<String> left = grams(a), right = grams(b);
        int common = 0;
        for (String gram : left) if (right.contains(gram)) common++;
        return common * 2.0 / (left.size() + right.size()) >= 0.55;
    }
    private static Set<String> grams(String value) {
        Set<String> result = new HashSet<String>();
        for (int i = 0; i + 3 <= value.length(); i++) result.add(value.substring(i, i + 3));
        return result;
    }
}
