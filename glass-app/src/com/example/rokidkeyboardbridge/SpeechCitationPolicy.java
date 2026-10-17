package com.example.rokidkeyboardbridge;

/** Spoken answers omit reference sections and URLs; displayed answers remain intact. */
public final class SpeechCitationPolicy {
    private SpeechCitationPolicy() {}
    public static String forSpeech(String raw) {
        if (raw == null) return "";
        String text = raw;
        java.util.regex.Matcher references = java.util.regex.Pattern.compile(
                "(?:情報元は|出典[：:]|参考リンク[：:])(?=[\\s\\S]*https?://)").matcher(text);
        if (references.find()) text = text.substring(0, references.start());
        text = text.replaceAll("\\[([^\\]]+)\\]\\(https?://[^\\s)]+\\)", "$1");
        text = text.replaceAll("https?://[^\\s<>\\\"、。]+", "");
        return text.replaceAll("[ \\t]{2,}", " ").trim();
    }
}
