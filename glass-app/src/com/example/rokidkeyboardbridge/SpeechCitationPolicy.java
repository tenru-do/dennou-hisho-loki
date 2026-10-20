package com.example.rokidkeyboardbridge;

/** Spoken answers omit reference sections and URLs; displayed answers remain intact. */
public final class SpeechCitationPolicy {
    private SpeechCitationPolicy() {}
    public static String forRequest(String prompt,String raw){String clean=forSpeech(raw);if(prompt==null||!prompt.matches("(?s).*(要点だけ|短く読んで|簡潔に読み上げ).*"))return clean;if(clean.length()<=600)return clean;int end=-1;for(int i=0;i<Math.min(600,clean.length());i++)if("。！？!?".indexOf(clean.charAt(i))>=0)end=i+1;return end<0?clean:clean.substring(0,end)+" 続きは画面で確認できます。";}
    public static String forSpeech(String raw) {
        if (raw == null) return "";
        String text = raw.replaceAll("\\[\\[[A-Z_]+(?::[^\\]\\r\\n]*)?\\]\\]", "");
        text=text.replaceAll("(?is)<(?:user_corrections|current_request|reference_context|image_reference|[^<>]*_policy)>.*?</[^<>]+>", "");
        text=text.replaceAll("(?m)^\\s*(?:情報元|出典|参考リンク)\\s*[:：].*$", "");
        java.util.regex.Matcher references = java.util.regex.Pattern.compile(
                "(?:情報元は|出典[：:]|参考リンク[：:])(?=[\\s\\S]*https?://)").matcher(text);
        if (references.find()) text = text.substring(0, references.start());
        text = text.replaceAll("\\[([^\\]]+)\\]\\(https?://[^\\s)]+\\)", "$1");
        text = text.replaceAll("https?://[^\\s<>\\\"、。]+", "");
        return text.replaceAll("[ \\t]{2,}", " ").trim();
    }
}
