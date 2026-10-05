package com.example.rokidgeminisecretary;
/** Generated continuous TOPIC scripts must contain two or three sentence units. */
final class TopicSpeechPolicy {
    static boolean accepts(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        int sentences = 0, start = 0;
        for (int i = 0; i < text.length(); i++) {
            if ("。！？!?".indexOf(text.charAt(i)) < 0) continue;
            while (i + 1 < text.length() && "。！？!?」』\"".indexOf(text.charAt(i + 1)) >= 0) i++;
            if (!text.substring(start, i + 1).trim().isEmpty()) sentences++;
            start = i + 1;
        }
        if (!text.substring(start).trim().isEmpty()) sentences++;
        return sentences >= 2 && sentences <= 3;
    }
}
