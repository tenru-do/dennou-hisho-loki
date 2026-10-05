package com.example.rokidgeminisecretary;

import java.util.HashSet;

final class CustomInformationMerge {
    private CustomInformationMerge() {}

    static String merge(String glass, String phone, int maxChars) {
        String master = glass == null ? "" : glass.trim();
        StringBuilder result = new StringBuilder(master);
        HashSet<String> lines = new HashSet<String>();
        for (String line : master.split("\\r?\\n")) lines.add(line.trim());
        for (String line : (phone == null ? "" : phone).split("\\r?\\n")) {
            String addition = line.trim();
            if (addition.isEmpty() || !lines.add(addition)) continue;
            if (result.length() > 0) result.append('\n');
            result.append(addition);
            if (result.length() > maxChars) return null;
        }
        return result.toString();
    }
}
