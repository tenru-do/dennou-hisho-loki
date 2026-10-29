package com.example.rokidgeminisecretary;
/** Generation targets 400-600 characters; modest delivery tolerance avoids dropping usable columns. */
final class TopicResponseLength {
    static boolean accepts(String text, boolean observed) {
        return observed ? text != null && text.length() >= 80 && text.length() <= 600
                : text != null && text.length() >= 350 && text.length() <= 750;
    }
    private TopicResponseLength() {}
}
