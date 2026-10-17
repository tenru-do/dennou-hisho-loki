package com.example.rokidkeyboardbridge;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** XML-safe SSML preparation. Rokid/Android string transports use the plain fallback. */
public final class MascotTtsTextFilter {
    private static final Pattern TOKENS = Pattern.compile("(?<![ぁ-んァ-ヶ一-龯])(?:マスター…|はぁっ|はあっ|だめ|あっ|あ…)(?=$|[\\s、。！？!?…])");
    private MascotTtsTextFilter() {}
    public static final class Prepared {
        public final String ssml, fallback;
        public final int requestedPauseMs;
        public final MascotSpeechVisualPolicy.Style style;
        private Prepared(String ssml,String fallback,int pause,MascotSpeechVisualPolicy.Style style) {
            this.ssml=ssml;this.fallback=fallback;this.requestedPauseMs=pause;this.style=style;
        }
        public String forTransport(boolean explicitlySupportsSsml) { return explicitlySupportsSsml?ssml:fallback; }
    }
    private static String escape(String value) {
        return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");
    }
    public static Prepared prepare(String raw) {
        String text=LokiVocalizationPolicy.normalize(SpeechCitationPolicy.forSpeech(raw));
        // Incoming markup is literal text, never trusted SSML. Discard illegal XML 1.0 controls.
        text=text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","");
        Matcher matcher=TOKENS.matcher(text);
        StringBuilder xml=new StringBuilder("<speak>"), plain=new StringBuilder();
        int at=0,pause=0;
        while(matcher.find()) {
            String between=text.substring(at,matcher.start()), token=matcher.group();
            xml.append(escape(between));plain.append(between);
            if(token.equals("マスター…")) {
                xml.append("<prosody rate=\"80%\" pitch=\"+1st\" volume=\"soft\">").append(escape(token)).append("</prosody>");
                plain.append("マスター、 ");
            } else if(token.equals("はぁっ")||token.equals("はあっ")||token.equals("だめ")) {
                xml.append("<prosody rate=\"110%\" pitch=\"+2st\">").append(escape(token)).append("</prosody><break time=\"150ms\"/>");
                plain.append(token).append("、 ");pause+=150;
            } else {
                xml.append(escape(token)).append("<break time=\"300ms\"/>");
                plain.append(token.equals("あ…")?"あ":token).append("。 ");pause+=300;
            }
            at=matcher.end();
        }
        xml.append(escape(text.substring(at))).append("</speak>");plain.append(text.substring(at));
        // Ensure literal markup cannot be read as XML tags by non-SSML engines.
        String fallback=plain.toString().replaceAll("<[^>]*>","").replace("<", " ").replace(">", " ")
                .replaceAll("[、。]\\s*[、。]", "。").trim();
        return new Prepared(xml.toString(),fallback,pause,MascotSpeechVisualPolicy.classify(text));
    }
}
