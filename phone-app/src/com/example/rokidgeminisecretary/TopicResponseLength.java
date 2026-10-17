package com.example.rokidgeminisecretary;
/** Sensor reactions are shorter than a knowledge column; never discard them for that alone. */
final class TopicResponseLength {
    static boolean accepts(String text, boolean observed) {
        return observed ? text != null && text.length() >= 80 && text.length() <= 600
                : ColumnSpeechFormatter.accepts(text);
    }
    private TopicResponseLength() {}
}
